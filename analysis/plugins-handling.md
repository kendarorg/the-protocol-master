# Plugin System Analysis - The Protocol Master

This document provides a comprehensive technical analysis of the plugin system used across all protocols in The Protocol Master (TPM), including DNS, HTTP, MySQL, Postgres, MongoDB, Redis, AMQP, and MQTT.

## 1. Plugin Architecture Overview

### 1.1 Core Design Principles

The plugin system is built on the **PF4J (Plugin Framework for Java)** library, which provides a standardized plugin management mechanism. The system implements a **phase-based execution model** where plugins can hook into various stages of protocol message handling:

- **PRE_CALL**: Before sending a request/message
- **CONNECT**: During connection establishment
- **POST_CALL**: After receiving a response
- **FINALIZE**: Final processing before returning to client
- **PRE_SOCKET_WRITE**: Before writing bytes to socket
- **ASYNC_RESPONSE**: Asynchronous response handling
- **NONE**: Plugin never executes

### 1.2 Plugin Type Hierarchy

```
BasePluginDescriptor (interface)
  ├── ProtocolPluginDescriptor (for protocol-specific plugins)
  └── GlobalPluginDescriptor (for global/cross-protocol plugins)

ProtocolPluginDescriptorBase (abstract class)
  ├── BasicRecordPlugin
  ├── BasicReplayPlugin
  ├── BasicMockPlugin
  ├── BasicRewritePlugin
  ├── BasicForwardPlugin
  ├── BasicLatencyPlugin
  ├── BasicNetworkErrorPlugin
  ├── BasicPercentPlugin
  ├── BasicReportPlugin
  └── BasicRestPluginsPlugin

GlobalPluginDescriptor implementations:
  └── GlobalReportPlugin
```

### 1.3 Core Classes Overview

| Class | Purpose |
|-------|---------|
| `PluginHandler` | Wraps a plugin and reflects its `handle()` method for execution |
| `PluginContext` | Execution context passed to each plugin during invocation |
| `Proxy` | Base proxy that manages plugin registration and execution |
| `NetworkProxy` | Network protocol proxy with plugin lifecycle management |
| `ProtoDescriptor` | Protocol descriptor that holds and initializes plugins |
| `TPMPluginsClassLoader` | Custom URLClassLoader for loading plugin classes into JTE templates |
| `PluginSettings` | Base class for plugin configuration |

---

## 2. Base Plugin Interfaces and Abstract Classes

### 2.1 BasePluginDescriptor Interface

**Location**: `protocol-common/src/main/java/org/kendar/plugins/base/BasePluginDescriptor.java`

```java
public interface BasePluginDescriptor<W extends PluginSettings> {
    String getId();                    // Unique plugin identifier
    Class<?> getSettingClass();        // Settings class for deserialization
    void terminate();                  // Cleanup on shutdown
    boolean isActive();                // Check if plugin is enabled
    void setActive(boolean active);    // Enable/disable plugin
    void refreshStatus();              // Refresh plugin status
}
```

**Key Properties**:
- `getId()`: Returns unique identifier like "record-plugin", "replay-plugin", etc.
- `getSettingClass()`: Returns the Class used to deserialize plugin configuration
- `isActive()`: Checks both internal flag and settings.isActive()
- `refreshStatus()`: Forces reinitialize if currently active

### 2.2 ProtocolPluginDescriptor Interface

**Location**: `protocol-common/src/main/java/org/kendar/plugins/base/ProtocolPluginDescriptor.java`

```java
public interface ProtocolPluginDescriptor<W extends PluginSettings> 
        extends ExtensionPoint, BasePluginDescriptor<W> {
    
    List<ProtocolPhase> getPhases();                      // Execution phases
    String getProtocol();                                 // Protocol name (http, mysql, etc.)
    ProtocolPluginDescriptor initialize(                  // Initialize with settings
        GlobalSettings global, 
        ProtocolSettings protocol, 
        PluginSettings pluginSetting);
    
    List<ProtocolPluginApiHandler> getApiHandler();       // REST API handlers
    ProtoDescriptor getProtocolInstance();                // Protocol instance
    void setProtocolInstance(ProtoDescriptor protocol);   // Set protocol instance
    W getSettings();                                       // Get plugin settings
}
```

**Key Methods**:
- `getPhases()`: Returns list of ProtocolPhase values where this plugin executes
- `initialize()`: Called during startup with global settings, protocol settings, and plugin-specific settings
- `getApiHandler()`: Returns list of API handlers for managing this plugin via REST API
- `getProtocolInstance()`: Returns the ProtoDescriptor this plugin is attached to

### 2.3 GlobalPluginDescriptor Interface

**Location**: `protocol-common/src/main/java/org/kendar/plugins/base/GlobalPluginDescriptor.java`

```java
public interface GlobalPluginDescriptor<W extends PluginSettings> 
        extends ExtensionPoint, BasePluginDescriptor<W> {
    
    GlobalPluginDescriptor initialize(
        GlobalSettings global, 
        PluginSettings pluginSettings);
    
    PluginSettings getSettings();
    BasePluginApiHandler getApiHandler();  // Single API handler (not a list)
}
```

**Use Cases**: Global plugins that apply across all protocols (e.g., GlobalReportPlugin).

### 2.4 ProtocolPluginDescriptorBase Abstract Class

**Location**: `protocol-common/src/main/java/org/kendar/plugins/base/ProtocolPluginDescriptorBase.java`

This is the primary abstract base class for protocol-specific plugins:

```java
public abstract class ProtocolPluginDescriptorBase<W extends PluginSettings> 
        implements ProtocolPluginDescriptor<W> {
    
    protected JsonMapper mapper;
    private boolean active;
    private String instanceId = "default";
    private List<ProtocolPluginApiHandler> apiHandler;
    private PluginSettings settings;
    private ProtoDescriptor protocolInstance;
    
    public ProtocolPluginDescriptor initialize(
        GlobalSettings global, 
        ProtocolSettings protocol, 
        PluginSettings pluginSetting) {
        
        this.instanceId = protocol.getProtocolInstanceId();
        this.settings = pluginSetting;
        if (settings != null) setActive(pluginSetting.isActive());
        return this;
    }
    
    public boolean isActive() {
        if (getSettings() != null) {
            return active && getSettings().isActive();
        }
        return active;
    }
    
    public void setActive(boolean active) {
        var isChanged = active != this.isActive();
        if (isChanged) handleActivation(active);
        this.active = active;
        if (getSettings() != null) {
            getSettings().setActive(active);
        }
        if (isChanged) handlePostActivation(active);
    }
    
    protected boolean handleSettingsChanged() { return true; }
    protected void handleActivation(boolean active) { }
    protected void handlePostActivation(boolean active) { }
}
```

**Key Features**:
- Provides template for `initialize()` with instance ID handling
- `setActive()` triggers `handleActivation()` and `handlePostActivation()` callbacks
- `isActive()` checks both internal flag AND settings flag (conjunction)
- `settings` can trigger `handleSettingsChanged()` callback when updated

### 2.5 ProtocolPhase Enum

**Location**: `protocol-common/src/main/java/org/kendar/plugins/base/ProtocolPhase.java`

```java
public enum ProtocolPhase {
    NONE("NONE"),                      // Never executes
    PRE_CALL("PRE_CALL"),              // Before sending request
    CONNECT("CONNECT"),                // During connection
    POST_CALL("POST_CALL"),            // After receiving response
    FINALIZE("FINALIZE"),              // Final processing
    PRE_SOCKET_WRITE("PRE_SOCKET_WRITE"),  // Before writing bytes
    ASYNC_RESPONSE("ASYNC_RESPONSE");      // Async response handling
}
```

---

## 3. Plugin Lifecycle

### 3.1 Loading Phase

**Occurs in**: `Main.java` (protocol-runner)

```java
// Step 1: Load plugins from plugins/ directory
pluginManager = new JarPluginManager(pathOfPluginsDir);
pluginManager.loadPlugins();  // Discovers .jar files

// Step 2: Start PF4J plugins
pluginManager.startPlugins();  // Calls Plugin.start() on each

// Step 3: Collect ClassLoaders and JAR URLs
var classLoaders = new HashMap<String, ClassLoader>();
var jarUrls = new HashSet<String>();
for (var plugin : pluginManager.getPlugins()) {
    jarUrls.add(plugin.getPluginPath().toUri().toURL().toString());
    var cl = plugin.getPluginClassLoader();
    classLoaders.put(cl.toString(), cl);
    // Bind extension classes to DI container
    for (var ec : pluginManager.getExtensionClasses(ExtensionPoint.class, plugin.getPluginId())) {
        diService.bind(ec);  // Register with dependency injection
    }
}

// Step 4: Create TPMPluginsClassLoader
var plcl = new TPMPluginsClassLoader(
    ClassLoader.getSystemClassLoader(),
    jarUrls.stream().map(u -> new URL(u)).toList(),
    classLoaders.values().toArray(new ClassLoader[0]));
```

### 3.2 Registration Phase

**Occurs in**: Protocol-specific initialization (e.g., `HttpProtocol`)

```java
@TpmConstructor
public HttpProtocol(GlobalSettings ini, HttpProtocolSettings settings,
                    @TpmNamed(tags = "http") List<BasePluginDescriptor> plugins) {
    
    // Step 1: Filter plugins - remove those without settings and non-AlwaysActivePlugin
    for (var i = plugins.size() - 1; i >= 0; i--) {
        var plugin = plugins.get(i);
        var specificPluginSetting = settings.getPlugin(
            plugin.getId(), 
            plugin.getSettingClass());
        
        // Only keep if settings exist OR plugin is AlwaysActivePlugin
        if (specificPluginSetting != null || 
            AlwaysActivePlugin.class.isAssignableFrom(plugin.getClass())) {
            
            // Step 2: Initialize each plugin
            ((ProtocolPluginDescriptor) plugin).initialize(ini, settings, specificPluginSetting);
            plugin.refreshStatus();
        } else {
            plugins.remove(i);  // Remove unplugins without configuration
        }
    }
    this.plugins = plugins;
}
```

### 3.3 Activation Phase

**Occurs in**: Proxy setup and message handling

```java
// In Proxy.setPluginHandlers()
public void setPluginHandlers(List<BasePluginDescriptor> filters) {
    for (var plugin : filters) {
        // Create PluginHandler wrappers
        var handlers = PluginHandler.of(plugin, this.protocol);
        for (var handler : handlers) {
            var pars = handler.getKey();  // "ClassName,ClassName"
            
            if (!allowedPlugins.containsKey(pars)) {
                allowedPlugins.put(pars, new HashMap<>());
            }
            
            var map = allowedPlugins.get(pars);
            for (var phase : ((ProtocolPluginDescriptor) plugin).getPhases()) {
                if (!map.containsKey(phase)) {
                    map.put(phase, new ArrayList<>());
                }
                map.get(phase).add(handler);  // Organize by phase
            }
        }
    }
}
```

### 3.4 Execution Phase

**Occurs in**: Message processing (e.g., `NetworkProxy.sendAndExpect()`)

```java
public <T extends ProtoState, K extends ReturnMessage> T sendAndExpect(
        NetworkProtoContext context,
        ProxyConnection connection,
        K of,
        T toRead) {
    
    long start = System.currentTimeMillis();
    var pluginContext = new PluginContext(getCaller(), of.getClass().getSimpleName(), start, context);
    
    // Step 1: PRE_CALL phase
    for (var plugin : getPluginHandlers(ProtocolPhase.PRE_CALL, of, toRead)) {
        if (plugin.handle(pluginContext, ProtocolPhase.PRE_CALL, of, toRead)) {
            return toRead;  // SHORT-CIRCUIT: skip actual call
        }
    }
    
    // Step 2: Actual protocol execution
    var sock = (WireProxySocket) connection.getConnection();
    sock.write(of, getProtocol().buildBuffer());
    var returnMessages = sock.read(toRead, false);
    for (var item : returnMessages) {
        if (toRead.getClass() == item.getClass()) {
            toRead = (T) item;
            break;
        }
    }
    
    // Step 3: POST_CALL phase
    for (var plugin : getPluginHandlers(ProtocolPhase.POST_CALL, of, toRead)) {
        if (plugin.handle(pluginContext, ProtocolPhase.POST_CALL, of, toRead)) {
            break;  // SHORT-CIRCUIT: stop further processing
        }
    }
    
    return toRead;
}
```

### 3.5 Termination Phase

**Occurs in**: Server shutdown

```java
public void terminate() {
    var terminatedPlugins = new HashSet<>();
    for (var i = plugins.size() - 1; i >= 0; i--) {
        var plugin = plugins.get(i);
        if (plugin.isActive() && !terminatedPlugins.contains(plugin)) {
            plugin.terminate();
            terminatedPlugins.add(plugin);
        }
    }
}
```

---

## 4. Plugin Context and Execution Environment

### 4.1 PluginContext Class

**Location**: `protocol-common/src/main/java/org/kendar/proxy/PluginContext.java`

```java
public class PluginContext {
    private static final AtomicLong counter = new AtomicLong(0);
    private final String type;
    private final ProtoContext context;
    private final Map<String, Object> tags = new HashMap<>();
    private long index;
    private long start;
    private String caller;
    
    public PluginContext(String caller, String type, long start, ProtoContext context) {
        this.caller = caller;
        this.type = type;
        this.context = context;
        this.index = counter.incrementAndGet();
        this.start = start;
    }
    
    public Map<String, Object> getTags() { return tags; }
    public ProtoContext getContext() { return context; }
    public String getType() { return type; }
    public long getIndex() { return index; }
    public long getStart() { return start; }
    public String getCaller() { return caller; }
    public int getContextId() { 
        if (getContext() == null) return 0;
        return getContext().getContextId();
    }
}
```

**Key Features**:
- `tags`: Shared dictionary for plugins to store state across phases
- `context`: ProtoContext containing connection and message state
- `type`: Message type (e.g., "Request", "Response")
- `index`: Global atomic counter for request tracking
- `start`: Timestamp for duration tracking
- `caller`: Protocol name (e.g., "http", "mysql")

### 4.2 PluginHandler Class

**Location**: `protocol-common/src/main/java/org/kendar/proxy/PluginHandler.java`

```java
public class PluginHandler {
    private final Class<?> typeIn;
    private final Class<?> typeOut;
    private final Method method;
    private final ProtocolPluginDescriptor target;
    
    public static List<PluginHandler> of(BasePluginDescriptor plugin, ProtoDescriptor protocol) {
        return of(plugin, "handle", protocol);
    }
    
    public static List<PluginHandler> of(BasePluginDescriptor plugin, String methodName, ProtoDescriptor protocol) {
        var result = new ArrayList<PluginHandler>();
        ((ProtocolPluginDescriptor) plugin).setProtocolInstance(protocol);
        var clazz = plugin.getClass();
        
        // Find all methods matching the name with 4 parameters
        var handles = Arrays.stream(clazz.getMethods())
            .filter(m -> m.getName().equalsIgnoreCase(methodName))
            .toList();
        
        for (var handle : handles) {
            if (handle.getParameterCount() != 4) continue;
            
            // Check parameter types: (PluginContext, ProtocolPhase, ?, ?)
            if (handle.getParameters()[0].getType() != PluginContext.class ||
                handle.getParameters()[1].getType() != ProtocolPhase.class) 
                continue;
            
            var inParam = handle.getParameters()[2].getType();
            var outParam = handle.getParameters()[3].getType();
            result.add(new PluginHandler((ProtocolPluginDescriptor) plugin, inParam, outParam, handle));
        }
        return result;
    }
    
    public boolean handle(PluginContext context, ProtocolPhase phase, Object in, Object out) {
        try {
            // Type-safe invocation
            if (in != null && typeIn.isAssignableFrom(in.getClass())) {
                if (out != null && typeOut.isAssignableFrom(out.getClass())) {
                    return (boolean) method.invoke(target, context, phase, in, out);
                } else if (out == null) {
                    return (boolean) method.invoke(target, context, phase, in, out);
                }
            } else if (in == null && out != null && typeOut.isAssignableFrom(out.getClass())) {
                return (boolean) method.invoke(target, context, phase, in, out);
            }
            return false;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PluginException(e);
        }
    }
}
```

**Key Features**:
- **Reflection-based dispatch**: Discovers and wraps `handle()` methods
- **Type checking**: Only invokes method if parameter types match
- **Return value**: `boolean` indicates whether to short-circuit further plugins
- **Key generation**: `typeIn.getName() + "," + typeOut.getName()` for lookup

---

## 5. Plugin Settings and Configuration

### 5.1 PluginSettings Base Class

**Location**: `protocol-common/src/main/java/org/kendar/settings/PluginSettings.java`

```java
public class PluginSettings {
    private String plugin;
    private boolean active;
    
    public String getPlugin() { return plugin; }
    public void setPlugin(String plugin) { this.plugin = plugin; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

**Subclasses**:
- `BasicRecordPluginSettings`
- `BasicReplayPluginSettings`
- `BasicMockPluginSettings`
- `RewritePluginSettings`
- `LatencyPluginSettings`
- `NetworkErrorPluginSettings`
- `BasicForwardPluginSettings`
- `BasicPercentPluginSettings`
- `BasicRestPluginsPluginSettings`

### 5.2 Configuration Flow

```
settings.yaml (YAML file)
    ↓
GlobalSettings.getPlugins() (global plugins)
    ↓
ProtocolSettings.getPlugins() (protocol-specific plugins)
    ↓
ProtocolSettings.getPlugin(pluginId, settingsClass)
    ↓
JsonMapper.deserialize() → PluginSettings subclass instance
```

Example:
```yaml
plugins:
  record-plugin:
    active: true
    ignoreTrivialCalls: true
    
protocols:
  http-01:
    plugins:
      record-plugin:
        active: true
        target:
          - "host1.com"
          - "host2.com"
```

---

## 6. Basic Plugin Types

### 6.1 BasicRecordPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicRecordPlugin.java`

```java
public abstract class BasicRecordPlugin<W extends BasicRecordPluginSettings> 
        extends ProtocolPluginDescriptorBase<W> {
    
    protected final StorageRepository repository;
    private final MultiTemplateEngine resolversFactory;
    private final SimpleParser parser;
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            switch (phase) {
                case PRE_CALL:
                    // Generate unique ID for this call
                    pluginContext.getTags().put("id", repository.generateIndex());
                    break;
                case POST_CALL:
                    // Record the completed call and response
                    postCall(pluginContext, in, out);
                    break;
                case ASYNC_RESPONSE:
                    // Record async response
                    pluginContext.getTags().put("id", repository.generateIndex());
                    asyncCall(pluginContext, out);
                    break;
            }
        }
        return false;
    }
    
    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var duration = System.currentTimeMillis() - pluginContext.getStart();
        var id = (long) pluginContext.getTags().get("id");
        
        JsonNode resSerialized = null;
        String resType = null;
        
        if (out != null) {
            resType = out.getClass().getSimpleName();
            resSerialized = mapper.toJsonNode(getData(out));
        }
        
        var storageItem = new StorageItem(
            pluginContext.getContextId(),
            mapper.toJsonNode(getData(in)),
            resSerialized,
            duration,
            pluginContext.getType(),
            pluginContext.getCaller(),
            in.getClass().getSimpleName(),
            resType);
        
        storageItem.setTimestamp(pluginContext.getStart());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        
        // Send to async queue for writing
        EventsQueue.send(new WriteItemEvent(
            new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
    }
    
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL, ProtocolPhase.ASYNC_RESPONSE);
    }
}
```

**Key Features**:
- Records all traffic to storage
- Phases: PRE_CALL (generate ID), POST_CALL (record), ASYNC_RESPONSE
- Stores input, output, duration, type, caller
- Can filter via `shouldNotSave()` and `ignoreTrivialCalls`
- Builds tags for indexing recorded items

### 6.2 BasicReplayPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicReplayPlugin.java`

```java
public abstract class BasicReplayPlugin<W extends BasicReplayPluginSettings> 
        extends ProtocolPluginDescriptorBase<W> {
    
    protected final HashSet<Integer> completedIndexes = new HashSet<>();
    protected final HashSet<Integer> completedOutIndexes = new HashSet<>();
    protected final StorageRepository repository;
    private final List<CompactLine> indexes = new ArrayList<>();
    private final List<CompactLine> repeatable = new ArrayList<>();
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase,
                          Object in, Object out) {
        if (isActive()) {
            if (out == null) {
                return sendAndForget(pluginContext, in) || getSettings().isBlockExternal();
            } else {
                return sendAndExpect(pluginContext, in, out) || getSettings().isBlockExternal();
            }
        }
        return false;
    }
    
    protected boolean sendAndExpect(PluginContext pluginContext, Object in, Object out) {
        // Look up recorded item matching the input
        var query = buildQuery(in);
        var recordedLine = repository.retrieve(query);
        
        if (recordedLine != null) {
            buildState(pluginContext, pluginContext.getContext(), in, out, out.getClass(), recordedLine);
            return true;
        }
        
        // Not found in recording
        if (getSettings().isBlockExternal()) {
            return true;  // Block and return empty/error
        }
        return false;  // Allow real call
    }
}
```

**Key Features**:
- Replays previously recorded messages
- Phases: PRE_CALL (find and replay), POST_CALL (update state)
- Matches based on tags (host, path, query for HTTP)
- Can block external calls or allow fallthrough
- Returns `true` to short-circuit actual network call

### 6.3 BasicMockPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicMockPlugin.java`

```java
public abstract class BasicMockPlugin<T, K> extends ProtocolPluginDescriptorBase<BasicMockPluginSettings> {
    protected final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();
    protected Map<String, MockStorage> mocks = new HashMap<>();
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object request, Object response) {
        if (!isActive()) return false;
        
        var matchingQuery = new ChangeableReference<>(0);
        var foundedIndex = new ChangeableReference<>(-1L);
        
        var withHost = firstCheckOnMainPart((T) request);
        withHost.forEach(a -> checkMatching(a, (T) request, matchingQuery, foundedIndex));
        
        if (foundedIndex.get() > 0) {
            var foundedResponse = mocks.values().stream()
                .filter(a -> a.getIndex() == foundedIndex.get())
                .findFirst();
            
            if (foundedResponse.isPresent()) {
                var founded = foundedResponse.get();
                counters.get(founded.getIndex()).getAndIncrement();
                
                // Support for "nth request" matching
                if (founded.getNthRequest() > 0) {
                    var isNth = counters.get(founded.getIndex()).get() == founded.getNthRequest();
                    if (isNth) {
                        founded.setNthRequest(-1);
                        if (founded.getCount() > 0) {
                            founded.setCount(founded.getCount() - 1);
                        }
                        writeOutput((T) request, (K) response, founded);
                        return true;
                    }
                    return false;
                } else if (founded.getCount() > 0) {
                    founded.setCount(founded.getCount() - 1);
                    writeOutput((T) request, (K) response, founded);
                    return true;
                }
            }
        }
        return false;
    }
}
```

**Key Features**:
- Serves pre-defined mock responses
- Supports request counting and nth-request matching
- Returns `true` to short-circuit and return mock response
- Requires protocol-specific `firstCheckOnMainPart()` and `checkMatching()`
- Counters allow single-use mocks or multi-use with limits

### 6.4 BasicRewritePlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicRewritePlugin.java`

```java
public abstract class BasicRewritePlugin<T, K, W extends RewritePluginSettings, J> 
        extends ProtocolPluginDescriptorBase<W> {
    
    private final List<ReplacerItemInstance> replacers = new ArrayList<>();
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object request, Object response) {
        if (!isActive()) return false;
        if (replacers.isEmpty()) return false;
        
        J toReplace = prepare((T) request, (K) response);
        for (var item : replacers) {
            replaceData(item, toReplace, (T) request, (K) response);
        }
        return false;  // Don't short-circuit
    }
    
    protected abstract J prepare(T request, K response);
    protected abstract void replaceData(ReplacerItemInstance item, J toReplace, T request, K response);
}
```

**Key Features**:
- Modifies request/response in-place
- Uses regex/template replacements
- Does NOT short-circuit (returns `false`)
- Protocol-specific implementations: HttpRewritePlugin, JdbcRewritePlugin, etc.

### 6.5 BasicLatencyPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicLatencyPlugin.java`

```java
public abstract class BasicLatencyPlugin<W extends LatencyPluginSettings> 
        extends BasicPercentPlugin<W> {
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (shouldRun() && in != null) {
            ChaosUtils.randomWait(getSettings().getMinMs(), getSettings().getMaxMs());
        }
        return false;
    }
    
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }
}
```

**Key Features**:
- Injects random delays
- Uses `BasicPercentPlugin.shouldRun()` for probabilistic execution
- Executes in PRE_CALL phase before actual call

### 6.6 BasicNetworkErrorPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicNetworkErrorPlugin.java`

```java
public abstract class BasicNetworkErrorPlugin<W extends BasicPercentPluginSettings> 
        extends BasicPercentPlugin<W> {
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, byte[] in, Object out) {
        if (shouldRun() && in != null && in.length > 0) {
            var modified = false;
            for (var i = 0; i < in.length; i++) {
                if (ChaosUtils.randomAction(5)) {
                    modified = true;
                    in[i] = (byte) ChaosUtils.randomBetween(0, 256);
                }
            }
            if (!modified) {
                in[0] = (byte) ChaosUtils.randomBetween(0, 256);
            }
        }
        return false;
    }
    
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_SOCKET_WRITE);
    }
}
```

**Key Features**:
- Corrupts bytes before sending to network
- Executes in PRE_SOCKET_WRITE phase
- Probabilistic corruption based on settings

### 6.7 BasicForwardPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicForwardPlugin.java`

```java
public abstract class BasicForwardPlugin extends ProtocolPluginDescriptorBase<BasicForwardPluginSettings> {
    private final AtomicReference<List<ForwardMatcher>> matchers = new AtomicReference<>(new ArrayList<>());
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, 
                         NetworkProtoContext in, Object out) {
        // Protocol-specific implementation
        // Matches source → target mappings
        // Forwards requests to alternative server
    }
}
```

**Key Features**:
- Routes requests to alternative servers
- Uses ForwardMatcher for pattern matching
- Protocol-specific implementations handle actual forwarding

### 6.8 BasicReportPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicReportPlugin.java`

```java
public abstract class BasicReportPlugin<W extends PluginSettings> 
        extends ProtocolPluginDescriptorBase<W> {
    
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL, ProtocolPhase.ASYNC_RESPONSE);
    }
    
    @Override
    public String getId() {
        return "report-plugin";
    }
}
```

**Key Features**:
- Abstract base for reporting/statistics
- Executes in POST_CALL and ASYNC_RESPONSE phases
- GlobalReportPlugin: cross-protocol reporting

### 6.9 BasicPercentPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicPercentPlugin.java`

```java
public abstract class BasicPercentPlugin<W extends BasicPercentPluginSettings> 
        extends ProtocolPluginDescriptorBase<W> {
    
    protected boolean shouldRun() {
        return isActive() && ChaosUtils.randomAction(getSettings().getPercentAction());
    }
}
```

**Key Features**:
- Base for percentage-based execution (LatencyPlugin, NetworkErrorPlugin, etc.)
- `shouldRun()` returns true based on configured percentage

### 6.10 BasicRestPluginsPlugin

**Location**: `protocol-common/src/main/java/org/kendar/plugins/BasicRestPluginsPlugin.java`

```java
public abstract class BasicRestPluginsPlugin extends ProtocolPluginDescriptorBase<BasicRestPluginsPluginSettings> {
    private final ConcurrentHashMap<ProtocolPhase, Map<String, List<RestPluginsInterceptor>>> interceptors = new ConcurrentHashMap<>();
    
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL,
                       ProtocolPhase.ASYNC_RESPONSE, ProtocolPhase.FINALIZE,
                       ProtocolPhase.PRE_SOCKET_WRITE, ProtocolPhase.CONNECT);
    }
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive()) {
            if (!interceptors.containsKey(phase)) return false;
            
            var possibleInterceptors = interceptors.get(phase);
            var inMatch = in == null ? "Object" : in.getClass().getSimpleName();
            var outMatch = out == null ? "Object" : out.getClass().getSimpleName();
            var key = inMatch + "." + outMatch;
            
            if (!possibleInterceptors.containsKey(key)) return false;
            
            var inSerialized = mapper.serialize(in);
            var outSerialized = mapper.serialize(out);
            
            for (var interceptor : possibleInterceptors.get(key)) {
                if (!interceptor.matches(inSerialized, outSerialized)) continue;
                // Call external REST API and modify in/out
            }
        }
        return false;
    }
}
```

**Key Features**:
- Calls external REST APIs to intercept and modify messages
- Can execute in any phase
- Matches based on message types and payload criteria

---

## 7. Protocol-Specific Plugin Overrides

### 7.1 HTTP Protocol Plugins

**Location**: `protocol-http/src/main/java/org/kendar/http/plugins/`

#### HttpRecordPlugin

```java
@TpmService(tags = "http")
public class HttpRecordPlugin extends BasicRecordPlugin<HttpRecordPluginSettings> {
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL);
    }
    
    @Override
    protected void postCall(PluginContext pluginContext, Object in, Object out) {
        var request = (Request) in;
        if (SiteMatcherUtils.matchSite((Request) in, target)) {
            if (getSettings().isRemoveEtags()) {
                // Remove caching headers
                request.getHeader("If-none-match").clear();
                request.getHeader("If-match").clear();
            }
            super.postCall(pluginContext, in, out);
        }
    }
    
    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var in = item.retrieveInAs(Request.class);
        return Map.of(
            "path", in.getPath(),
            "host", in.getHost(),
            "query", buildQueryString(in));
    }
    
    @Override
    public String getProtocol() {
        return "http";
    }
}
```

**Protocol-Specific Additions**:
- Filters by hostname/path matching
- Removes ETags for stable recording
- Records path and host as tags for indexing

#### HttpReplayPlugin

```java
@TpmService(tags = "http")
public class HttpReplayPlugin extends BasicReplayPlugin<HttpReplayPluginSettings> {
    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (isActive() && phase == ProtocolPhase.PRE_CALL) {
            var request = (Request) in;
            var response = (Response) out;
            
            if (SiteMatcherUtils.matchSite(request, target)) {
                var sent = doSend(pluginContext, request, response);
                if (!sent && blockExternal) {
                    response.setStatusCode(404);
                    response.setResponseText(new TextNode("Page Not Found: " + request.buildUrl()));
                    return true;
                }
                return sent;
            }
        }
        return false;
    }
    
    @Override
    protected int tagsMatching(Map<String, String> tags, Map<String, String> query) {
        if (!tags.get("path").equalsIgnoreCase(query.get("path"))) return -1;
        if (!tags.get("host").equalsIgnoreCase(query.get("host"))) return -1;
        return super.tagsMatching(tags, query);
    }
}
```

**Protocol-Specific Additions**:
- Matches requests by host and path
- Sets 404 response for external blocking
- Executes only in PRE_CALL phase

### 7.2 JDBC Protocol Plugins (MySQL, Postgres)

**Location**: `protocol-common-jdbc/src/main/java/org/kendar/plugins/`

#### JdbcRecordPlugin

```java
public abstract class JdbcRecordPlugin extends BasicRecordPlugin<BasicRecordPluginSettings> {
    @Override
    protected void postCall(PluginContext pluginContext, Object obIn, Object obOUt) {
        JdbcCall in = (JdbcCall) obIn;
        SelectResult out = (SelectResult) obOUt;
        
        var req = new JdbcRequest(in.getQuery(), in.getParameterValues());
        JdbcResponse res = out.isIntResult() 
            ? new JdbcResponse(out.getCount())
            : new JdbcResponse(out);
        
        var storageItem = new StorageItem(
            pluginContext.getContextId(),
            req, res, duration,
            pluginContext.getType(),
            pluginContext.getCaller(),
            "JdbcCall", "SelectResult");
        
        EventsQueue.send(new WriteItemEvent(
            new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
    }
    
    protected boolean shouldNotSaveJdbc(StorageItem in, CompactLine out) {
        var result = in.retrieveInAs(JdbcRequest.class);
        return result.getQuery().trim().toLowerCase().startsWith("set");
    }
}
```

**Protocol-Specific Additions**:
- Handles JdbcCall and SelectResult types
- Filters SET statements (connection setup)
- Distinguishes result sets from update counts

#### JdbcReplayPlugin

```java
public abstract class JdbcReplayPlugin extends BasicReplayPlugin<BasicReplayPluginSettings> {
    @Override
    protected Map<String, String> buildTag(Object in) {
        var jdbcCall = (JdbcCall) in;
        var result = new HashMap<String, String>();
        result.put("query", jdbcCall.getQuery());
        
        var tokenized = getParser().tokenize(jdbcCall.getQuery())
            .stream()
            .filter(a -> a.getType() != TokenType.VALUE_ITEM)
            .map(SimpleToken::getValue)
            .collect(Collectors.toList());
        
        result.put("tokenized", String.join(" ", tokenized));
        result.put("parametersCount", jdbcCall.getParameterValues().size() + "");
        return result;
    }
    
    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, 
                             Object in, Object outputItem, Object aClass, LineToRead lineToRead) {
        var outObj = (SelectResult) aClass;
        if (lineToRead != null && lineToRead.getStorageItem() != null) {
            var source = lineToRead.getStorageItem().retrieveOutAs(JdbcResponse.class);
            outObj.fill(source.getSelectResult());
        }
    }
}
```

**Protocol-Specific Additions**:
- Tags queries by tokenized form (values removed)
- Handles PreparedStatement parameter counts
- Rebuilds SelectResult from stored response

### 7.3 MongoDB Plugins

**Location**: `protocol-mongo/src/main/java/org/kendar/mongo/plugins/`

#### MongoRecordPlugin

```java
@TpmService(tags = "mongodb")
public class MongoRecordPlugin extends BasicRecordPlugin<BasicRecordPluginSettings> {
    @Override
    protected Object getData(Object of) {
        if (of instanceof BaseMessageData) {
            return ((BaseMessageData) of).serialize();
        }
        return of;
    }
    
    @Override
    public String getProtocol() {
        return "mongodb";
    }
}
```

**Protocol-Specific Additions**:
- Serializes BaseMessageData to plain format
- Overrides `getData()` for custom serialization

### 7.4 Other Protocol Plugins

Similar pattern for:
- **RedisRecordPlugin/ReplayPlugin**: Handle RESP protocol
- **MqttRecordPlugin/ReplayPlugin**: Handle MQTT messages
- **Amqp091RecordPlugin/ReplayPlugin**: Handle AMQP frames

---

## 8. Plugin Loading (TPMPluginsClassLoader)

**Location**: `protocol-common/src/main/java/org/kendar/utils/TPMPluginsClassLoader.java`

```java
public class TPMPluginsClassLoader extends URLClassLoader {
    private final ClassLoader[] classLoaders;
    private final ConcurrentHashMap<String, ClassLoader> classLoaderMap = new ConcurrentHashMap<>();
    
    public TPMPluginsClassLoader(ClassLoader parent, List<URL> urls, ClassLoader... classLoaders) {
        super(urls.toArray(new URL[]{}), parent);
        this.classLoaders = classLoaders;
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Check cache first
        if (classLoaderMap.containsKey(name)) {
            return classLoaderMap.get(name).loadClass(name);
        }
        
        Class<?> result = null;
        
        // Try plugin classloaders
        for (ClassLoader classLoader : classLoaders) {
            try {
                result = classLoader.loadClass(name);
                classLoaderMap.put(name, classLoader);
                return result;
            } catch (Exception ignored) { }
        }
        
        // Try parent classloader
        try {
            result = getParent().loadClass(name);
            classLoaderMap.put(name, this.getParent());
            return result;
        } catch (Exception ignored) { }
        
        // Fall back to URLClassLoader
        return super.loadClass(name, resolve);
    }
}
```

**Purpose**: 
- Allows JTE templates to access plugin classes
- Maintains cache of which ClassLoader provides which class
- Delegates to parent for non-plugin classes
- Enables multiple plugin JAR versions

---

## 9. Plugin Execution Pipeline

### 9.1 Complete Request Flow

```
Client sends message
        ↓
NetworkProxy.sendAndExpect() called
        ↓
Create PluginContext
        ↓
PRE_CALL PHASE:
  for each plugin in getPluginHandlers(PRE_CALL, in, out):
    if plugin.handle(context, PRE_CALL, in, out) returns TRUE:
      RETURN OUT (short-circuit, skip real call)
        ↓
ACTUAL PROTOCOL CALL:
  send message to server
  read response from server
        ↓
POST_CALL PHASE:
  for each plugin in getPluginHandlers(POST_CALL, in, out):
    if plugin.handle(context, POST_CALL, in, out) returns TRUE:
      BREAK (stop plugin chain, but return result)
        ↓
ASYNC_RESPONSE PHASE (if applicable):
  for each plugin in getPluginHandlers(ASYNC_RESPONSE, null, publish):
    if plugin.handle(context, ASYNC_RESPONSE, null, publish) returns TRUE:
      BREAK
        ↓
Return result to client
```

### 9.2 Plugin Handler Lookup and Execution

```java
public <I, J> List<PluginHandler> getPluginHandlers(ProtocolPhase phase, I in, J out) {
    var data = String.join(",", in.getClass().getName(), out.getClass().getName());
    var anonymousData = String.join(",", Object.class.getName(), Object.class.getName());
    
    var forData = allowedPlugins.get(anonymousData);  // Try generic Object,Object
    var result = new ArrayList<PluginHandler>();
    
    if (forData != null) {
        var forPhase = forData.get(phase);
        if (forPhase != null) {
            result.addAll(forPhase);  // Add all generic plugins
        }
    }
    
    forData = allowedPlugins.get(data);  // Try specific types
    if (forData != null) {
        var forPhase = forData.get(phase);
        if (forPhase != null) {
            result.addAll(forPhase);  // Add type-specific plugins
        }
    }
    
    return result;  // Both generic and specific, in that order
}
```

**Lookup Strategy**:
1. First find plugins that handle (Object, Object)
2. Then find plugins that handle specific types
3. Execute in order of registration

### 9.3 Short-Circuit Behavior

```
PRE_CALL plugins:
  - Returning TRUE skips actual call and returns early
  - Use cases: Mock, Replay, Block

POST_CALL plugins:
  - Returning TRUE breaks chain but returns result
  - Use cases: Record, Report, Transform

ASYNC_RESPONSE plugins:
  - Returning TRUE breaks chain
  - Use cases: Record, Report
```

---

## 10. Sample Plugins

### 10.1 Plugin File Structure

Each plugin JAR contains:

```
plugin-name/
  ├── pom.xml
  ├── src/main/java/
  │   └── org/kendar/sample/plugins/
  │       ├── SamplePlugin.java (extends Plugin, implements TPMPluginFile)
  │       ├── HttpFilter.java (extends ProtocolPluginDescriptorBase)
  │       └── HttpFilterSettings.java (extends PluginSettings)
  ├── src/main/resources/
  │   ├── plugin.properties
  │   ├── plugin-id.properties
  │   └── plugin_name.version
  └── target/
      └── plugin-name.jar
```

### 10.2 Example: HttpFilter Sample Plugin

**Location**: `sample-plugins/src/main/java/org/kendar/sample/plugins/HttpFilter.java`

```java
@Extension
@TpmService(tags = "http")
public class HttpFilter extends ProtocolPluginDescriptorBase<HttpFilterSettings> 
        implements AlwaysActivePlugin {
    
    public HttpFilter(JsonMapper mapper) {
        super(mapper);
    }
    
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, 
                         Request in, Response out) {
        // Example: Log all HTTP requests
        log.info("Request: {} {}", in.getMethod(), in.getPath());
        return false;  // Don't short-circuit
    }
    
    @Override
    public Class<?> getSettingClass() {
        return HttpFilterSettings.class;
    }
    
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }
    
    @Override
    public String getId() {
        return "sample-http";
    }
    
    @Override
    public String getProtocol() {
        return "http";
    }
}
```

### 10.3 Plugin Discovery and Registration

```java
@Extension  // PF4J annotation - marks as discoverable
@TpmService(tags = "http")  // DI annotation - tags for protocol
public class HttpFilter extends ProtocolPluginDescriptorBase<HttpFilterSettings> ...
```

**Discovery Process**:
1. PF4J scans JAR for @Extension classes implementing ExtensionPoint
2. DI system discovers @TpmService classes
3. Constructor injection receives dependencies
4. Plugin is registered with protocol tag

---

## 11. PluginSettings and Configuration

### 11.1 PluginSettings Class Hierarchy

```
PluginSettings (base)
  ├── BasicRecordPluginSettings
  │   └── active: boolean
  │       ignoreTrivialCalls: boolean
  │       target: List<TargetMatcher>
  ├── BasicReplayPluginSettings
  │   └── active: boolean
  │       blockExternal: boolean
  │       target: List<TargetMatcher>
  ├── BasicMockPluginSettings
  │   └── active: boolean
  ├── RewritePluginSettings
  │   └── active: boolean
  ├── LatencyPluginSettings
  │   └── active: boolean
  │       minMs: int
  │       maxMs: int
  │       percentAction: int
  ├── NetworkErrorPluginSettings
  │   └── active: boolean
  │       percentAction: int
  ├── BasicForwardPluginSettings
  │   └── active: boolean
  │       mappings: Map<String, String>
  └── BasicRestPluginsPluginSettings
      └── active: boolean
          interceptors: List<RestPluginsInterceptor>
```

### 11.2 Configuration Example

```yaml
plugins:
  global-report-plugin:
    active: true

protocols:
  http-01:
    plugins:
      record-plugin:
        active: true
        ignoreTrivialCalls: true
        target:
          - pattern: ".*\.example\.com.*"
      replay-plugin:
        active: false
      mock-plugin:
        active: true
      rewrite-plugin:
        active: true
      latency-plugin:
        active: true
        minMs: 100
        maxMs: 500
        percentAction: 25
      forward-plugin:
        active: true
        mappings:
          localhost:8080: example.com:80
```

---

## 12. Plugin Activation and Deactivation

### 12.1 Activation Flow

```java
public void setActive(boolean active) {
    var isChanged = active != this.isActive();
    
    if (isChanged) {
        handleActivation(active);  // Pre-activation hook
    }
    
    this.active = active;
    
    if (getSettings() != null) {
        getSettings().setActive(active);  // Sync with settings
    }
    
    if (isChanged) {
        handlePostActivation(active);  // Post-activation hook
    }
}
```

### 12.2 Example: BasicReplayPlugin Activation

```java
@Override
protected void handleActivation(boolean active) {
    try {
        if (this.isActive() != active) {
            getSettings().setActive(active);
            completedOutIndexes.clear();  // Reset state
            completedIndexes.clear();
            
            if (active) {
                loadIndexes();  // Load recorded data
            } else {
                // Cleanup
            }
        }
    } catch (Exception e) {
        log.error("Error activating replay plugin", e);
    }
}
```

---

## 13. Return Values and Short-Circuit Semantics

### 13.1 Return Value Interpretation

```
Plugin.handle() returns:
  
  TRUE  → Short-circuit (skip remaining plugins and/or real call)
  FALSE → Continue with next plugin or proceed
```

### 13.2 Per-Phase Behavior

| Phase | Returns TRUE | Returns FALSE |
|-------|-------------|---------------|
| PRE_CALL | Skip real call, return cached result | Continue to next plugin |
| CONNECT | Close connection | Continue |
| POST_CALL | Break chain, return result | Continue to next plugin |
| FINALIZE | Break chain, finalize | Continue |
| PRE_SOCKET_WRITE | Stop writing | Continue |
| ASYNC_RESPONSE | Stop processing response | Continue |

### 13.3 Example: Replay Short-Circuit

```java
// BasicReplayPlugin.handle()
public boolean handle(PluginContext pluginContext, ProtocolPhase phase, 
                      Object in, Object out) {
    if (isActive()) {
        if (out == null) {
            return sendAndForget(pluginContext, in) || getSettings().isBlockExternal();
        } else {
            return sendAndExpect(pluginContext, in, out) || getSettings().isBlockExternal();
        }
    }
    return false;
}

// If found in recording: return true (skip real call)
// If not found and blockExternal: return true (return 404)
// If not found and !blockExternal: return false (allow real call)
```

---

## 14. Advanced Features

### 14.1 Plugin State via PluginContext.tags

```java
// In BasicRecordPlugin.handle()
case PRE_CALL:
    pluginContext.getTags().put("id", repository.generateIndex());
    break;

// Later in BasicRecordPlugin.postCall()
var id = (long) pluginContext.getTags().get("id");
EventsQueue.send(new WriteItemEvent(
    new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
```

**Use Cases**:
- Sharing state between PRE_CALL and POST_CALL phases
- Tracking unique identifiers
- Building tag maps for indexing

### 14.2 Plugin Tag Matching

```java
// HTTP plugins tag recordings
@Override
public Map<String, String> buildTag(StorageItem item) {
    var in = item.retrieveInAs(Request.class);
    return Map.of(
        "path", in.getPath(),
        "host", in.getHost(),
        "query", buildQueryString(in));
}

// Later, when replaying
@Override
protected int tagsMatching(Map<String, String> tags, Map<String, String> query) {
    if (!tags.get("path").equalsIgnoreCase(query.get("path"))) return -1;
    if (!tags.get("host").equalsIgnoreCase(query.get("host"))) return -1;
    return super.tagsMatching(tags, query);
}
```

### 14.3 AlwaysActivePlugin Interface

```java
public interface AlwaysActivePlugin { }

// Used in protocol initialization
if (specificPluginSetting != null || 
    AlwaysActivePlugin.class.isAssignableFrom(plugin.getClass())) {
    ((ProtocolPluginDescriptor) plugin).initialize(ini, settings, specificPluginSetting);
}
```

**Effect**: AlwaysActivePlugin plugins don't require settings to be registered.

### 14.4 Plugin Async Events

```java
// BasicRecordPlugin sends async events
EventsQueue.send(new WriteItemEvent(new LineToWrite(...)));

// BasicReplayPlugin listens for completion
EventsQueue.register("main", (e) -> {
    completedIndexes.clear();
    completedOutIndexes.clear();
}, ReplayStatusEvent.class);
```

---

## 15. Plugin Initialization in Protocols

### 15.1 HTTP Protocol Initialization Example

```java
@TpmConstructor
public HttpProtocol(GlobalSettings ini, HttpProtocolSettings settings,
                    @TpmNamed(tags = "http") List<BasePluginDescriptor> plugins) {
    
    // DI injects all plugins tagged with "http"
    for (var i = plugins.size() - 1; i >= 0; i--) {
        var plugin = plugins.get(i);
        
        // Get plugin settings from protocol config
        var specificPluginSetting = settings.getPlugin(
            plugin.getId(), 
            plugin.getSettingClass());
        
        if (specificPluginSetting != null || 
            AlwaysActivePlugin.class.isAssignableFrom(plugin.getClass())) {
            
            // Initialize plugin
            ((ProtocolPluginDescriptor) plugin).initialize(ini, settings, specificPluginSetting);
            plugin.refreshStatus();
        } else {
            // Remove plugin without configuration
            plugins.remove(i);
        }
    }
    
    this.plugins = plugins;
}
```

### 15.2 Protocol Settings Plugin Lookup

```java
public class ProtocolSettings {
    private Map<String, Object> plugins = new HashMap<>();
    
    public PluginSettings getPlugin(String pluginId, Class<?> clazz) {
        if (!plugins.containsKey(pluginId)) {
            return null;
        }
        
        var protocolData = plugins.get(pluginId);
        // Deserialize from raw map to typed settings class
        return (PluginSettings) mapper.deserialize(
            mapper.serialize(protocolData), 
            clazz);
    }
}
```

---

## 16. Complete Plugin Execution Example

### 16.1 HTTP Request with Multiple Plugins

```
Client: GET /api/users HTTP/1.1
        Host: example.com

        ↓ (request deserialized to Request object)

1. PRE_CALL Phase:
   
   Plugin: RewritePlugin
   - Modifies request headers
   - Returns false → continue
   
   Plugin: ReplayPlugin (enabled)
   - Tags: host=example.com, path=/api/users
   - Finds recording in storage
   - Returns true → SHORT-CIRCUIT
   - Response: {"users": [...]} (from recording)
   
   (No actual network call)

2. POST_CALL Phase:
   
   Plugin: RecordPlugin
   - Generates ID: 12345
   - Stores request/response/duration to disk
   - Returns false → continue
   
   Plugin: ReportPlugin
   - Updates statistics
   - Returns false → continue

Output: Recorded response sent to client
```

### 16.2 SQL Query with JDBC Plugins

```
Client: executeQuery("SELECT * FROM users WHERE id = ?", [1])

        ↓ (converted to JdbcCall object)

1. PRE_CALL Phase:
   
   Plugin: RecordPlugin
   - Generates ID: 67890
   - Stores to tags: id = 67890
   - Returns false → continue

2. ACTUAL CALL:
   
   JDBC Driver executes: SELECT * FROM users WHERE id = 1
   Returns: ResultSet with user data

3. POST_CALL Phase:
   
   Plugin: RecordPlugin
   - Retrieves ID: 67890 (from tags)
   - Stores JdbcRequest + JdbcResponse to disk
   - Tags: query="SELECT * FROM users WHERE id = ?", parametersCount=1
   - Returns false → continue
   
   Plugin: ReportPlugin
   - Increments query count
   - Returns false → continue

Output: ResultSet returned to client
```

---

## 17. Summary

The plugin system in The Protocol Master provides a flexible, extensible architecture for intercepting and modifying protocol traffic. Key characteristics:

1. **Phase-based execution**: Plugins hook into PRE_CALL, POST_CALL, etc.
2. **Type-safe reflection**: PluginHandler uses reflection to invoke typed `handle()` methods
3. **Short-circuit capability**: Plugins can return `true` to skip remaining plugins or actual calls
4. **Per-protocol customization**: Each protocol can override base plugin classes
5. **Tag-based state sharing**: PluginContext.tags share state between phases
6. **Configuration-driven**: YAML settings control plugin activation and behavior
7. **DI integration**: Plugins are discovered and injected via tag-based DI
8. **Async event system**: EventsQueue enables cross-plugin communication
9. **Custom ClassLoader**: TPMPluginsClassLoader enables JTE template access to plugin classes
10. **Lifecycle hooks**: `handleActivation()`, `handlePostActivation()`, `handleSettingsChanged()`

---

**Document Generated**: May 2026
**Version**: 1.0
