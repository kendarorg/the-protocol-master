# Plan: Add a protocol as a plugin jar (the `protocol-dns-plugin` pattern)

This document describes how to ship a protocol as a self-contained PF4J plugin
jar (dropped into `target/plugins`) instead of compiling it into the runner
classpath. It is protocol-agnostic: substitute your protocol name `x`
(package `org.kendar.x`) throughout.

## Background — what makes a module a "plugin jar"

Built-in protocols (postgres, amqp-091, …) are ordinary modules on the runner's
classpath. A **plugin** is the same kind of module but packaged as a
self-contained PF4J jar dropped into `target/plugins`.

At startup, `protocol-runner`'s `Main.java` (lines ~101–138) does:

```
var pluginsDir = settings.get().getPluginsDir();
pluginManager = new JarPluginManager(pathOfPluginsDir);
pluginManager.loadPlugins();
pluginManager.startPlugins();
for (var plugin : pluginManager.getPlugins()) {
    for (var ec : pluginManager.getExtensionClasses(ExtensionPoint.class, plugin.getPluginId())) {
        diService.bind(ec);   // <-- every ExtensionPoint class bound into DI
    }
}
```

After binding, a plugin protocol behaves exactly like a compiled-in one — DI
wires it by its `@TpmService(tags=...)`.

Three ingredients turn a module into a loadable plugin jar:

1. A `pom.xml` that assembles a jar-with-dependencies carrying PF4J manifest
   entries and copies it to `../target/plugins`.
2. A `Plugin-Class` entry point extending `org.pf4j.Plugin` + `TPMPluginFile`.
3. `@Extension @TpmService`-annotated `ExtensionPoint` classes (descriptor,
   settings, CLI, resolver, APIs) picked up by PF4J's
   `ExtensionAnnotationProcessor`.

## Steps

### 1. Create the module skeleton

Mirror `protocol-dns-plugin/`:

```
protocol-x-plugin/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/org/kendar/x/
    │   │   ├── XPlugin.java              # Plugin-Class entry point
    │   │   ├── XProtocol.java            # protocol descriptor / logic
    │   │   ├── XProtocolSettings.java    # config
    │   │   ├── XCliHandler.java          # CLI options
    │   │   ├── XJteResolver.java         # UI resolver
    │   │   └── apis/XApis.java           # (optional) REST endpoints
    │   └── resources/
    │       ├── protocol_x_plugin.version
    │       └── jte/x/protocol.jte
    └── test/java/org/kendar/x/XProtocolTest.java
```

### 2. Register the module in the root `pom.xml`

Add `<module>protocol-x-plugin</module>` in **both** `<modules>` lists — the
default profile (~line 180) and the `deploy` profile (~line 205). Place it
before `protocol-runner` so the plugin builds first.

### 3. Write `pom.xml`

Copy `protocol-dns-plugin/pom.xml` verbatim and change only:

- `<artifactId>protocol-x-plugin</artifactId>`, `<name>` / `<description>`.
- Properties: `plugin.id` = `protocol-x-plugin`, `plugin.class` =
  `org.kendar.x.XPlugin`.
- The two `<include>protocol-dns-plugin.jar</include>` → `protocol-x-plugin.jar`.
- The antrun `setup-version` echo file path → `protocol_x_plugin.version`.
- Swap the `dnsjava` dependency for your protocol's library — **all** of
  `pf4j`, `protocol-common`, and protocol libs stay `<scope>provided</scope>`
  with `slf4j-api` excluded (so they aren't loaded twice under the plugin
  classloader).

Keep intact:

- The `maven-compiler-plugin` with the
  `org.pf4j.processor.ExtensionAnnotationProcessor` annotation processor.
- The `maven-assembly-plugin` (jar-with-dependencies) writing the `Plugin-Id`,
  `Plugin-Version`, `Plugin-Provider`, `Plugin-Class`, `Plugin-Dependencies`
  manifest entries.
- The `maven-deploy-plugin` `skip=true`.
- The `install`-phase `maven-resources-plugin` copy of
  `protocol-x-plugin.jar` into `../target/plugins`.

### 4. Write the classes (copy DNS equivalents, rename)

- **`XPlugin`** `extends Plugin implements TPMPluginFile` —
  `getTpmPluginName()` returns `"protocol-x-plugin"`, `getTpmPluginVersion()`
  reads `/protocol_x_plugin.version`. This is the manifest `Plugin-Class`.
- **`XProtocolSettings`** `extends ProtocolSettings implements ExtensionPoint`,
  annotated `@Extension @TpmService(tags="x")` — config fields (port, etc.).
- **`XProtocol`** `extends NetworkProtoDescriptor implements ExtensionPoint`,
  `@Extension @TpmService(tags="x")` — `@TpmConstructor` taking
  `@TpmNamed(tags="x") List<BasePluginDescriptor> plugins`; implement
  `getPort()`, `start()`, `terminate()`, `createContext()`, and plugin-phase
  handling. (Wrapper-style like DNS → override `isWrapper()`; proxy-style →
  follow `protocol-amqp-091` instead.) This holds the real protocol logic.
- **`XCliHandler`** `extends NetworkProtocolCommandLineHandler implements
  ExtensionPoint`, `@Extension @TpmService(tags="x")` — `getId()` /
  `getDescription()` / `buildProtocolSettings()` + custom `CommandOption`s.
- **`XJteResolver`** `extends JteResolver`, `@Extension @TpmService` — one-line
  constructor passing its own classloader.
- **`XApis` (optional)** `implements ProtocolApiHandler`, returned from
  `XProtocol.getApiHandler()`.

### 5. Resources

- `protocol_x_plugin.version` — seed with the current version string; the
  antrun step rewrites it at build.
- `jte/x/protocol.jte` — UI page (copy DNS's, adjust).

### 6. Build & verify

- `mvn -pl protocol-x-plugin -am install` → check
  `target/plugins/protocol-x-plugin.jar` exists and its manifest carries
  correct `Plugin-Id` / `Plugin-Class` / `Plugin-Version`.
- Start the runner: confirm the log shows the plugin loaded, the CLI exposes the
  new protocol's options, and there is no classloader / `provided`-scope
  duplication error.
- Add `XProtocolTest` following `DnsProtocolTest`.

## Key gotchas

- Every DI-discovered class must be `@Extension` **and** carry
  `@TpmService(tags="x")` with a consistent tag — that tag is how the runner
  groups CLI handlers, plugins, and the descriptor together.
- `provided` scope on shared deps is mandatory; bundling `protocol-common` /
  `pf4j` into the jar breaks the plugin classloader.
- Register the module in **both** profiles, or `deploy` builds silently omit it.
- The jar filename in the `<include>` and the `plugin.id` must match what you
  reference; the `install`-phase copy step filters by the literal jar name.

## Reference files

- `protocol-dns-plugin/pom.xml` — the plugin packaging template.
- `protocol-dns-plugin/src/main/java/org/kendar/dns/DnsPlugin.java` — entry point.
- `protocol-dns-plugin/src/main/java/org/kendar/dns/DnsProtocol.java` — descriptor/logic.
- `protocol-dns-plugin/src/main/java/org/kendar/dns/DnsProtocolSettings.java` — settings.
- `protocol-dns-plugin/src/main/java/org/kendar/dns/DnsCliHandler.java` — CLI.
- `protocol-dns-plugin/src/main/java/org/kendar/dns/DnsJteResolver.java` — UI resolver.
- `protocol-runner/src/main/java/org/kendar/Main.java` (~101–138) — plugin load/bind.
