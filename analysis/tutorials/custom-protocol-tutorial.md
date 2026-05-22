# Tutorial: Building a Custom Protocol for TPM

**Protocol**: `TJSON` — Tiny JSON-over-TCP  
**Goal**: Intercept, record, replay and manipulate a simple JSON RPC protocol.

---

## Wire Format

Each TCP frame:
```
[4 bytes, big-endian uint32: payload length][JSON bytes]
```

Four message types, distinguished by the `"type"` field:

| Direction | Type | Example |
|---|---|---|
| Client → Server | `REQUEST` | `{"type":"REQUEST","id":1,"method":"echo","payload":{"text":"hi"}}` |
| Server → Client | `RESPONSE` | `{"type":"RESPONSE","id":1,"result":{"text":"hi"}}` |
| Server → Client | `ERROR` | `{"type":"ERROR","id":1,"code":400,"message":"unknown method"}` |
| Server → Client (push) | `CALLBACK` | `{"type":"CALLBACK","event":"update","data":{"value":42}}` |

This protocol intentionally covers: framing, JSON parse, request-response, server-push callbacks, and error states.

---

## Module Layout

```
protocol-tjson/
  pom.xml
  src/
    main/java/org/kendar/tjson/
      TJsonProtocol.java            ← NetworkProtoDescriptor
      TJsonContext.java             ← per-connection state
      TJsonProtocolSettings.java    ← settings POJO
      TJsonProxy.java               ← proxy socket factory
      cli/
        TJsonCommandLineHandler.java
      codec/
        TJsonFrame.java             ← wire codec (encode/decode)
      fsm/
        events/
          TJsonMessageEvent.java    ← parsed message event
        BaseTJsonState.java         ← common state base
        Handshake.java              ← optional initial hello
        Request.java                ← CLIENT→SERVER request
        Response.java               ← SERVER→CLIENT response
        ErrorResponse.java          ← SERVER→CLIENT error
        Callback.java               ← SERVER→CLIENT push
        Disconnect.java             ← connection teardown
      plugins/
        TJsonRecordPlugin.java
        TJsonReplayPlugin.java
        TJsonJsonManipulatorPlugin.java   ← custom example
        TJsonJsonManipulatorSettings.java
    test/java/org/kendar/runner/
      TJsonBasicTest.java
      TJsonRecordReplayTest.java
      TJsonCallbackTest.java
      utils/
        TJsonTestServer.java        ← fake TCP server for tests
```

---

## Step 1 — `pom.xml`

Inherit from the parent, depend on `protocol-common`, and use `protocol-test` in test scope:

```xml
<parent>
    <groupId>org.kendar.protocol</groupId>
    <artifactId>the-protocol-master</artifactId>
    <version>${revision}</version>
</parent>
<artifactId>protocol-tjson</artifactId>

<dependencies>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-common</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-test</artifactId>
        <version>${revision}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Also add to the root `pom.xml` `<modules>` list: `<module>protocol-tjson</module>`.  
Add to `protocol-runner/pom.xml` as a dependency so the runner can discover it.

---

## Step 2 — Settings (`TJsonProtocolSettings.java`)

Extend `ByteProtocolSettingsWithLogin` (provides `port`, `connectionString`, `login`, `password`).  
Tag `"tjson"` ties this settings class to the protocol via DI:

```java
@TpmService(tags = "tjson")
public class TJsonProtocolSettings extends ByteProtocolSettingsWithLogin {
    public TJsonProtocolSettings() {
        setProtocol("tjson");
    }
}
```

`settings.json` entry:
```json
"tjson-01": {
    "protocol": "tjson",
    "port": 7070,
    "connectionString": "localhost:7071"
}
```

---

## Step 3 — Wire Codec (`TJsonFrame.java`)

Codec lives outside the FSM — pure encode/decode, no state:

```java
public class TJsonFrame {
    // Reads 4-byte length header then the JSON bytes from a BBuffer.
    // Returns null if not enough bytes yet (caller must accumulate).
    public static JsonNode decode(BBuffer buf) {
        if (buf.size() - buf.getPosition() < 4) return null;
        buf.mark();
        int len = buf.getInt();           // big-endian 4 bytes
        if (buf.size() - buf.getPosition() < len) {
            buf.reset();                  // not enough bytes yet
            return null;
        }
        byte[] payload = new byte[len];
        buf.read(payload);
        return new ObjectMapper().readTree(payload);
    }

    // Writes [4-byte length][JSON bytes] into a new BBuffer.
    public static BBuffer encode(Object message) {
        byte[] json = new ObjectMapper().writeValueAsBytes(message);
        var buf = new BBuffer(BBufferEndianness.BE);
        buf.writeInt(json.length);
        buf.write(json);
        return buf;
    }
}
```

**Why separate codec?** Tests can exercise encode/decode without standing up the FSM. The FSM states call `TJsonFrame.decode()` from inside `executeFrame()`.

---

## Step 4 — Event (`TJsonMessageEvent.java`)

Events carry one parsed JSON message from the `BytesEvent` translator into typed FSM states:

```java
public class TJsonMessageEvent extends BaseEvent {
    private final JsonNode message;
    private final String type;   // REQUEST, RESPONSE, ERROR, CALLBACK

    public TJsonMessageEvent(ProtoContext context, NetworkProtoDescriptor descriptor,
                             JsonNode message) {
        super(context, descriptor);
        this.message  = message;
        this.type     = message.path("type").asText("UNKNOWN");
    }
    // getters only
}
```

---

## Step 5 — Message Translator

Before the main loop, register a "translator" interrupt state that converts raw `BytesEvent` (bytes from Netty) into `TJsonMessageEvent`. This is the same pattern as `MqttPacketTranslator`:

```java
public class TJsonMessageTranslator extends ProtoState {
    public TJsonMessageTranslator() { super(BytesEvent.class); }

    @Override
    public boolean canRun(BytesEvent event) { return true; }

    @Override
    public Iterator<ProtoStep> execute(BytesEvent event) {
        var ctx    = (TJsonContext) event.getContext();
        var buf    = ctx.getAccumulationBuffer();
        buf.write(event.getBuffer().getAll());
        var node = TJsonFrame.decode(buf);
        if (node == null) return iteratorOfNothing(); // wait for more bytes
        return iteratorOfList(new TJsonMessageEvent(ctx,
                event.getDescriptor(), node));
    }
}
```

**Key point**: `BBuffer.mark()` / `reset()` let you peek without consuming bytes when a frame is incomplete.

---

## Step 6 — Context (`TJsonContext.java`)

Holds per-connection mutable state:

```java
public class TJsonContext extends NetworkProtoContext {
    private final BBuffer accumulationBuffer =
            new BBuffer(BBufferEndianness.BE);

    public TJsonContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    public BBuffer getAccumulationBuffer() { return accumulationBuffer; }

    @Override
    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new BBuffer(BBufferEndianness.BE);
    }

    @Override
    public void disconnect(Object connection) {
        super.disconnect(connection);
        var conn = (ProxyConnection) getValue("CONNECTION");
        if (conn != null && conn.getConnection() != null) {
            ((WireProxySocket) conn.getConnection()).close();
        }
    }
}
```

Store any cross-request data (e.g. pending request IDs for correlation) as a field here or via `setValue("KEY", value)` for items plugins may also need to read.

---

## Step 7 — FSM States

### `BaseTJsonState.java`

```java
public abstract class BaseTJsonState extends ProtoState
        implements NetworkReturnMessage {
    // canRun checks the "type" field of TJsonMessageEvent
    public boolean canRun(TJsonMessageEvent event) {
        return getExpectedType().equals(event.getType());
    }
    protected abstract String getExpectedType();

    @Override
    public void write(BBuffer buf) {
        // Serialises this state back to wire for replay/proxy
        TJsonFrame.encode(this).transferTo(buf);
    }
}
```

### `Request.java` — client request (loop body, **core state**)

```java
public class Request extends BaseTJsonState {
    private int id;
    private String method;
    private JsonNode payload;

    public Request(Class<?>... events) { super(events); }

    @Override protected String getExpectedType() { return "REQUEST"; }

    @Override
    public Iterator<ProtoStep> execute(TJsonMessageEvent event) {
        var node     = event.getMessage();
        id      = node.path("id").asInt();
        method  = node.path("method").asText();
        payload = node.path("payload");

        var ctx        = (TJsonContext) event.getContext();
        var proxy      = (TJsonProxy) ctx.getProxy();
        var connection = (ProxyConnection) ctx.getValue("CONNECTION");

        // Forward to real server, expect one response/error back
        return iteratorOfRunnable(() ->
            proxy.sendAndExpect(ctx, connection, this, new Response())
        );
    }
}
```

`proxy.sendAndExpect()` sends the request to the real server and blocks until the expected response type arrives. This is the normal **synchronous request-response** path.

### `Response.java` — server response

```java
public class Response extends BaseTJsonState {
    private int id;
    private JsonNode result;

    public Response(Class<?>... events) { super(events); }
    public Response() { super(); }

    @Override protected String getExpectedType() { return "RESPONSE"; }

    @Override
    public Iterator<ProtoStep> execute(TJsonMessageEvent event) {
        var node = event.getMessage();
        id     = node.path("id").asInt();
        result = node.path("result");
        // Return to client — write() is called by the FSM engine
        return iteratorOfList(this);
    }
}
```

### `ErrorResponse.java` — error handling

```java
public class ErrorResponse extends BaseTJsonState {
    private int id;
    private int code;
    private String message;

    @Override protected String getExpectedType() { return "ERROR"; }

    @Override
    public Iterator<ProtoStep> execute(TJsonMessageEvent event) {
        var node = event.getMessage();
        id      = node.path("id").asInt();
        code    = node.path("code").asInt();
        message = node.path("message").asText();
        // Pass error back to client unchanged
        return iteratorOfList(this);
    }
}
```

**Retry logic** — if you need automatic retries on errors, the cleanest place is in a plugin (see Step 11) or wrap `sendAndExpect` in a loop inside the state:

```java
// inside Request.execute(), retry up to 3 times
return iteratorOfRunnable(() -> {
    int attempts = 0;
    Exception lastEx = null;
    while (attempts++ < 3) {
        try {
            proxy.sendAndExpect(ctx, connection, this, new Response());
            return;
        } catch (ProxyException ex) {
            lastEx = ex;
            Sleeper.sleep(200);
        }
    }
    throw new RuntimeException("TJSON request failed after 3 attempts", lastEx);
});
```

### `Callback.java` — server-push (unsolicited message)

Callbacks arrive from the server outside a request-response cycle. Register `Callback` as an **interrupt state** in the protocol descriptor:

```java
public class Callback extends BaseTJsonState {
    private String event;
    private JsonNode data;

    @Override protected String getExpectedType() { return "CALLBACK"; }

    @Override
    public Iterator<ProtoStep> execute(TJsonMessageEvent event) {
        var node  = event.getMessage();
        this.event = node.path("event").asText();
        this.data  = node.path("data");
        var ctx    = (TJsonContext) event.getContext();
        var proxy  = (TJsonProxy) ctx.getProxy();
        // Push to client — proxy.respond writes directly back without a request
        proxy.respond(this,
            new PluginContext("TJSON", "CALLBACK",
                System.currentTimeMillis(), ctx));
        return iteratorOfList(this);
    }
}
```

---

## Step 8 — Protocol Descriptor (`TJsonProtocol.java`)

This is where loops, conditions, and interrupts are assembled:

```java
@TpmService(tags = "tjson")
public class TJsonProtocol extends NetworkProtoDescriptor {

    @TpmConstructor
    public TJsonProtocol(GlobalSettings ini, TJsonProtocolSettings settings,
                         TJsonProxy proxy,
                         @TpmNamed(tags = "tjson") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
    }

    @Override
    public boolean isBe() { return true; }   // big-endian

    @Override
    public int getPort() { return port; }

    @Override
    protected void initializeProtocol() {
        // Interrupt states fire on ANY received frame before the main FSM
        addInterruptState(new TJsonMessageTranslator(BytesEvent.class)); // bytes → event
        addInterruptState(new Callback(TJsonMessageEvent.class));        // server push

        initialize(
            new ProtoStateSequence(
                // LOOP: keep processing until disconnect
                new ProtoStateWhile(
                    new ProtoStateSwitchCase(
                        // Request → expect Response OR Error
                        new ProtoStateSequence(
                            new Request(TJsonMessageEvent.class),
                            new ProtoStateSwitchCase(
                                new Response(TJsonMessageEvent.class),
                                new ErrorResponse(TJsonMessageEvent.class)
                            )
                        )
                    )
                )
            )
        );
    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor pd, int contextId) {
        return new TJsonContext(pd, contextId);
    }
}
```

**FSM building blocks cheat-sheet:**

| Class | Meaning |
|---|---|
| `ProtoStateSequence(A, B, C)` | Execute A then B then C in order |
| `ProtoStateWhile(body)` | Repeat `body` until connection closes or interrupt fires |
| `ProtoStateSwitchCase(A, B)` | Try A; if `canRun()` fails, try B |
| `addInterruptState(S)` | S fires on every event before the main stack is checked |
| `.asOptional()` | Skip this state without error if `canRun()` returns false |
| `.asProxy()` | This message is forwarded transparently (no plugin dispatch) |

---

## Step 9 — Proxy (`TJsonProxy.java`)

```java
@TpmService
public class TJsonProxy extends NetworkProxy {

    @TpmConstructor
    public TJsonProxy(@TpmNamed(tags = "tjson") ByteProtocolSettingsWithLogin settings) {
        super(settings.getConnectionString(),
              settings.getLogin(), settings.getPassword(), false);
    }

    // Used in unit tests
    public TJsonProxy(String connectionString) {
        super(connectionString, null, null);
    }

    @Override
    protected WireProxySocket buildProxyConnection(NetworkProtoContext ctx,
            InetSocketAddress addr, AsynchronousChannelGroup group) {
        try {
            return new BasicWireProxySocket(ctx,
                new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new ProxyException(e);
        }
    }

    @Override
    protected String getCaller() { return "TJSON"; }
}
```

`BasicWireProxySocket` (from `protocol-common`) handles async NIO — reuse it unless the protocol requires custom framing on the upstream leg.

---

## Step 10 — CLI Handler (`TJsonCommandLineHandler.java`)

Exposes `-p tjson` sub-commands so the proxy can be started from the CLI:

```java
@TpmService(tags = "tjson")
public class TJsonCommandLineHandler extends ProtocolCommandLineHandler {
    @TpmConstructor
    public TJsonCommandLineHandler(TJsonProtocolSettings settings) {
        super(settings);
    }

    @Override
    public String getProtocolTag() { return "tjson"; }

    @Override
    protected void addExtraOptions(CommandOptions options) {
        // add protocol-specific CLI flags here if needed
    }
}
```

---

## Step 11 — Record Plugin (`TJsonRecordPlugin.java`)

Extends `BasicRecordPlugin` and provides tag extraction for index queries:

```java
@TpmService(tags = "tjson")
public class TJsonRecordPlugin
        extends BasicRecordPlugin<BasicRecordPluginSettings> {

    public TJsonRecordPlugin(JsonMapper mapper, StorageRepository storage,
                             MultiTemplateEngine tpl, SimpleParser parser) {
        super(mapper, storage, tpl, parser);
    }

    @Override public Class<?> getSettingClass() {
        return BasicRecordPluginSettings.class;
    }

    @Override public String getProtocol() { return "tjson"; }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var tags = new HashMap<String, String>();
        var in = mapper.toJsonNode(item.getInput());
        tags.put("type",   in.path("type").asText());
        tags.put("method", in.path("method").asText(""));
        tags.put("id",     in.path("id").asText(""));
        return tags;
    }
}
```

Enable recording in `settings.json`:
```json
"tjson-01": {
    "protocol": "tjson",
    "port": 7070,
    "connectionString": "localhost:7071",
    "plugins": {
        "record-plugin": { "active": true }
    }
}
```

---

## Step 12 — Replay Plugin (`TJsonReplayPlugin.java`)

```java
@TpmService(tags = "tjson")
public class TJsonReplayPlugin
        extends BasicReplayPlugin<BasicReplayPluginSettings> {

    public TJsonReplayPlugin(JsonMapper mapper, StorageRepository storage,
                             MultiTemplateEngine tpl, SimpleParser parser) {
        super(mapper, storage, tpl, parser);
    }

    @Override public Class<?> getSettingClass() {
        return BasicReplayPluginSettings.class;
    }

    @Override public String getProtocol() { return "tjson"; }
}
```

During replay the `PRE_CALL` hook intercepts every `Request` before it reaches the proxy, finds the matching stored `Response` by tags (`type=REQUEST`, `method=echo`), deserializes it, and returns it to the client without touching the real server.

Enable replay:
```json
"plugins": {
    "replay-plugin": { "active": true }
}
```

---

## Step 13 — Custom Plugin: JSON Manipulator

Real-world example: rewrite the `payload.text` field of every `echo` request before forwarding.

```java
@TpmService(tags = "tjson")
public class TJsonJsonManipulatorPlugin
        extends ProtocolPluginDescriptorBase<TJsonJsonManipulatorSettings> {

    public TJsonJsonManipulatorPlugin(JsonMapper mapper) { super(mapper); }

    @Override public String getId()        { return "tjson-manipulator"; }
    @Override public String getProtocol()  { return "tjson"; }

    @Override public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override public Class<?> getSettingClass() {
        return TJsonJsonManipulatorSettings.class;
    }

    public boolean handle(PluginContext ctx, ProtocolPhase phase,
                          Request in, Response out) {
        if (!isActive()) return false;
        if (!"echo".equals(in.getMethod())) return false;
        var settings = (TJsonJsonManipulatorSettings) getSettings();
        // Mutate payload in-place
        ((ObjectNode) in.getPayload()).put("text", settings.getReplacementText());
        return false; // false = continue plugin chain and proxy
    }

    @Override public Class<?> getInputType()  { return Request.class; }
    @Override public Class<?> getOutputType() { return Response.class; }
}
```

```json
"plugins": {
    "tjson-manipulator": {
        "active": true,
        "replacementText": "intercepted!"
    }
}
```

---

## Step 14 — Testing

### Test server (`TJsonTestServer.java`)

A minimal blocking `ServerSocket` that echoes back any valid request:

```java
public class TJsonTestServer implements Closeable {
    private final ServerSocket server;
    private final Thread thread;

    public TJsonTestServer(int port) throws IOException {
        server = new ServerSocket(port);
        thread = new Thread(() -> {
            try (var client = server.accept()) {
                var in  = client.getInputStream();
                var out = client.getOutputStream();
                while (true) {
                    byte[] lenBuf = in.readNBytes(4);
                    int len = ByteBuffer.wrap(lenBuf).getInt();
                    byte[] payload = in.readNBytes(len);
                    var req  = new ObjectMapper().readTree(payload);
                    var resp = Map.of("type","RESPONSE","id",
                                     req.path("id").asInt(),
                                     "result", req.path("payload"));
                    byte[] respBytes = new ObjectMapper().writeValueAsBytes(resp);
                    var buf = ByteBuffer.allocate(4 + respBytes.length);
                    buf.putInt(respBytes.length);
                    buf.put(respBytes);
                    out.write(buf.array());
                }
            } catch (Exception ignored) {}
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override public void close() throws IOException { server.close(); }
}
```

### Basic integration test (`TJsonBasicTest.java`)

```java
class TJsonBasicTest {
    private TJsonTestServer testServer;

    @BeforeEach
    void start() throws Exception {
        testServer = new TJsonTestServer(7071);
        Main.execute(new String[]{
            "-p", "tjson", "-port", "7070",
            "-connectionString", "localhost:7071"
        });
        Awaitility.await().until(Main::isRunning);
    }

    @AfterEach
    void stop() throws Exception {
        Main.stop();
        testServer.close();
    }

    @Test
    void echoRequest_roundTrips() throws Exception {
        // Connect to proxy, send REQUEST, assert RESPONSE
        try (var sock = new Socket("localhost", 7070)) {
            var req = Map.of("type","REQUEST","id",1,
                             "method","echo","payload",Map.of("text","hello"));
            send(sock, req);
            var resp = receive(sock);
            assertEquals("RESPONSE", resp.path("type").asText());
            assertEquals("hello", resp.path("result").path("text").asText());
        }
    }
}
```

### Record-replay test (`TJsonRecordReplayTest.java`)

Pattern (same as `MultiRecordReplayTest`):
1. Start proxy with `record-plugin` active.
2. Send several requests through the proxy.
3. Stop proxy; assert storage has expected item count via `GET /api/global/storage/index`.
4. Restart proxy with `replay-plugin` active; real server offline.
5. Repeat same requests; assert same responses arrive (no proxy call to real server).

### Callback test (`TJsonCallbackTest.java`)

1. Start proxy and test server.
2. Connect client.
3. Test server sends a `CALLBACK` frame after 100 ms.
4. Assert client receives the callback message.

---

## Step 15 — Debugging

**Log FSM events** — set `logLevel: "DEBUG"` in `settings.json`. Every `canRun()` call and state transition is logged.

**Dump raw bytes** — in any state's `execute()`, call:
```java
log.debug("raw buffer: {}", BBuffer.hexDump(event.getBuffer().getAll()));
```

**Break on accumulation** — if frames arrive fragmented, add an assertion in `TJsonMessageTranslator`:
```java
log.trace("accumulation buffer size={}, position={}",
          buf.size(), buf.getPosition());
```

**Inspect stored recordings** — after recording, call:
```
GET http://localhost:5005/api/global/storage/index
GET http://localhost:5005/api/global/storage/item/tjson/0
```
Or browse the web UI at `http://localhost:5005/recording`.

**TPMql queries** for filtering:
```
GET /api/global/storage/index?tpmql=tags.method=='echo'
```

---

## Step 16 — Best Practices

| Concern | Recommendation |
|---|---|
| **Frame accumulation** | Always buffer in `TJsonContext`; never assume one `BytesEvent` = one frame |
| **Interrupt states** | Register translator and server-push (callback) as `addInterruptState()`, not inside the main loop |
| **Error vs exception** | Protocol errors (e.g. `ERROR` frames) → model as a state; Java exceptions → only for hard failures (closed socket, corrupt frame) |
| **Retry in state vs plugin** | Retry on transient errors in the state (it owns the request-response pair); use a plugin for cross-cutting policies (e.g. rate limiting) |
| **Tag granularity** | Index tags should uniquely identify a (request type, logical key) pair; over-tagging causes replay mismatches |
| **Settings mutability** | Keep `ProtocolSettings` and `PluginSettings` as plain Jackson-serializable POJOs; avoid circular references |
| **Test server** | Keep `TJsonTestServer` strictly minimal; don't simulate real server business logic — that belongs in the recorded storage |
| **`canRun()` discipline** | Each state must accept exactly one message type; overlapping `canRun()` conditions inside a `ProtoStateSwitchCase` cause silent wrong-branch execution |
| **`asOptional()` vs `SwitchCase`** | Use `asOptional()` for truly optional messages (e.g. a version header that may be absent); use `SwitchCase` when the message type determines control flow |

---

## Full `settings.json` Example

```json
{
    "pluginsDir": "target/plugins",
    "logLevel":   "INFO",
    "dataDir":    "file=target/data",
    "apiPort":    5005,
    "plugins": {
        "report-plugin": { "active": true }
    },
    "protocols": {
        "tjson-01": {
            "protocol":          "tjson",
            "port":              7070,
            "connectionString":  "localhost:7071",
            "timeoutSeconds":    30,
            "plugins": {
                "record-plugin": { "active": false },
                "replay-plugin": { "active": false },
                "tjson-manipulator": {
                    "active": false,
                    "replacementText": "intercepted!"
                }
            }
        }
    }
}
```

---

## Implementation Sequence

1. `pom.xml` + add to root modules + add to `protocol-runner` deps
2. `TJsonProtocolSettings`
3. `TJsonFrame` codec (unit-test it standalone)
4. `TJsonMessageEvent`
5. `TJsonContext`
6. `TJsonMessageTranslator`
7. `BaseTJsonState`, `Request`, `Response`, `ErrorResponse`
8. `TJsonProxy`
9. `TJsonProtocol.initializeProtocol()` (no callbacks yet)
10. `TJsonTestServer` + `TJsonBasicTest` — get round-trip working
11. `Callback` + interrupt registration + `TJsonCallbackTest`
12. `TJsonRecordPlugin` + `TJsonReplayPlugin` + `TJsonRecordReplayTest`
13. `TJsonCommandLineHandler`
14. `TJsonJsonManipulatorPlugin` + plugin test
15. Wire up `settings.json` and smoke-test via web UI
