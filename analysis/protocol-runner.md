# `protocol-runner` Module — Technical Analysis

## 1. Module Purpose and Responsibilities

`protocol-runner` is the **executable entry point** for *The Protocol Master (TPM)*, a multi-protocol proxy that can transparently intercept, record, and replay traffic for a variety of backend protocols. Its responsibilities are:

- Parse command-line arguments or a JSON configuration file.
- Load external protocol plug-ins via PF4J from a configurable `plugins/` directory.
- Start one Netty-based TCP server per configured protocol instance (HTTP, HTTPS, PostgreSQL, MySQL, MongoDB, Redis, MQTT, AMQP 0.9.1, DNS, …).
- Expose a management HTTP server (`com.sun.net.httpserver.HttpServer`) on a configurable API port, serving:
  - A REST JSON API (Swagger-documented).
  - An HTMX-driven Bootstrap web UI backed by JTE templates.
  - Swagger UI static assets.
- Host a global plugin system (reporting, future global plugins).
- Persist recorded scenarios to a pluggable storage layer (file system, encrypted, …).
- React to lifecycle events (terminate, restart, storage-reload) through an internal event bus.

The artifact is packaged by `maven-assembly-plugin` as a fat JAR (`protocol-runner.jar`) with `org.kendar.Main` as the manifest main class.

---

## 2. Entry Point (`Main.java`) and Startup Flow

```
Main.main(String[])
  └─ loop: execute(args) until terminate/no-restart
        └─ execute(String[])   ← static, re-entrant
              1. Create DiService, scan "org.kendar" package
              2. ProtocolsRunner.getMainOptions() → build CLI option tree
              3. CommandParser.parseIgnoreMissing(args) (first pass, no protocol sub-options yet)
              4. Load PF4J JarPluginManager from pluginsDir
                 - pluginManager.loadPlugins() / startPlugins()
                 - Register all ExtensionPoint classes in DiService
                 - Build TPMPluginsClassLoader (unified class loader)
              5. If no -cfg flag: enumerate ProtocolCommandLineHandler / PluginCommandLineHandler
                 and attach sub-options, then CommandParser.parse(args) (second pass)
              6. execute(GlobalSettings, unattended) ← actual boot
```

Inside `execute(GlobalSettings, unattended)`:

1. **Event bus setup** — registers handlers for `TerminateEvent`, `RestartEvent`, `StorageReloadedEvent`.
2. **Log level** — applies Logback `Level` from `settings.logLevel` (default `INFO`).
3. **Storage** — resolves `StorageRepository` implementation from `dataDir` prefix (e.g. `file=data`, `encrypted=data`).
4. **Protocol startup (parallel)** — for each entry in `settings.protocols`, a new thread:
   - Creates a child DI scope (`TpmScopeType.THREAD`).
   - Deserializes typed `ProtocolSettings` for that protocol.
   - Resolves `NetworkProtoDescriptor` (the protocol state machine) via DI.
   - Calls `descriptor.initialize()`.
   - Wraps in `NettyServer` and calls `ps.start()`.
   - Waits up to 5 s for `isRunning()`.
   - Registers the resulting `ProtocolInstance` with `ApiHandler`.
   - Collects per-plugin API handler filters.
5. **CountDownLatch** waits until all protocol threads complete.
6. **Global plugins** — instantiates all `GlobalPluginDescriptor` beans, calls `initialize()` for each that has settings.
7. **API HTTP server** (in a separate thread) — collects all registered API filters, calls `ApiFiltersLoader.loadFilters()`, then starts `HttpServer` on `settings.apiPort`.
8. **Interactive mode** — if not unattended, starts a thread reading `Q` (quit) or `R` (restart) from stdin.
9. **Spin loop** — `while (!terminateReceived && !restartReceived) Sleeper.sleep(100)`.

**Restart semantics**: when a `StorageReloadedEvent` arrives with a settings path, `Main` will re-enter `execute()` using the new settings file as `-cfg`.

**Stop**: `Main.stop()` calls `stopInternal()`, which stops all Netty servers, clears the cache, stops the `HttpServer`, cleans the `EventsQueue`, and cleans the `DiService`.

---

## 3. `ProtocolsRunner` — Protocol Loading and Configuration

`ProtocolsRunner` (annotated `@TpmService`) is a thin utility used by `Main`:

- **`getMainOptions()`** — builds the `CommandOptions` root tree. It discovers all `StorageCli` beans (storage back-ends) and prints their descriptions in the help text. Core options:

  | Short | Long           | Description                                  |
  |-------|----------------|----------------------------------------------|
  | `-cfg`| `--config`     | Load a JSON config file                      |
  | `-un` | `--unattended` | No stdin Q/R prompt                          |
  | `-dd` | `--datadir`    | Storage path (type=path)                     |
  | `-ll` | `--loglevel`   | Log level                                    |
  | `-ap` | `--apis`       | API server port (0 = disabled)               |
  | `-pld`| `--pluginsDir` | Directory for PF4J plugin JARs               |
  | `-p`  | `--protocol`   | Protocol sub-command (populated dynamically) |
  | `-h`  | `--help`       | Show help (optional: protocol name filter)   |

- **`loadConfigFile()`** — reads a JSON file into `GlobalSettings`. Supports environment-variable driven token replacement: if `TPM_REPLACE=a=b,c=d` is set, every occurrence of `%a%` in the JSON is replaced with `b`, etc.

- **`getOrDefault()`** — simple null-safe default helper.

Protocol instances are started inside `Main.execute()` using the DI container to resolve the correct `NetworkProtoDescriptor` tag matching the `protocol` field of each entry. The `ProtocolCommandLineHandler` and `PluginCommandLineHandler` classes (not in this module, but discovered via DI) attach protocol-specific CLI sub-options dynamically to the `-p` command.

---

## 4. Web UI Layer

### Template Engine

The web UI uses two template technologies:

- **JTE** (`gg.jte` / `io.marioslab.basis.template`): server-side HTML templates with `.jte` extension, resolved at runtime from classpath via `RunnerJteResolver` (extends `JteResolver`, a `@TpmService`) which uses the module's own classloader.
- **HTMX**: the browser-side JavaScript library (`htmx.org`) handles partial page updates — individual HTMX endpoints return HTML fragments, not full pages, allowing accordion/tab updates without full reloads.
- **Bootstrap 3**: used for styling (panels, accordions, buttons, form controls).

`MultiTemplateEngine` (from a shared library, injected via DI) delegates to `RunnerJteResolver` (and potentially other resolvers from plugins). It is called as `resolversFactory.render("template.jte", model, response)`.

`BaseTemplate` (in `org.kendar.tpl`) is an abstract base class for older template-based handlers; it caches loaded `Template` objects in a `ConcurrentHashMap`.

### HTMX Handler Classes

#### `MainHtmxPages`
- `GET /` → renders `index.jte` with `GlobalSettings` as model.
- The index page shows: API port, data dir, plugins dir, terminate/restart buttons, download settings link.

#### `ProtocolsHtmx`
- `GET /protocols` → reads sorted `GlobalSettings.protocols`, renders `protocols.jte`.
- `GET /protocols/{protocolId}` → same view (the client-side JS selects the right entry).

#### `PluginsHtmx`
- `GET /globalpl` → collects all `GlobalPluginDescriptor` beans, renders `globalpl.jte`.
- `GET /plugins` → sorted list of all `ProtocolInstance`, renders `plugins.jte`.
- `GET /plugins/wildcard` → same data, renders `plugins/wildcard.jte` (wildcard plugin control across all protocols).
- `GET /plugins/active` → same data, renders `plugins/active.jte`.
- `GET /plugins/protocols?protocolId=X` → single-protocol plugin list, renders `plugins/protocol.jte`.
- `GET /plugins/{protocolInstanceId}/{pluginId}` → single plugin detail page. For `instanceId=global` renders `plugins/singlegl.jte`, otherwise `plugins/single.jte`. Model includes serialized plugin settings JSON for editing.

#### `RecordingHtmx`
- `GET /recording` → renders `recording.jte` (search form shell).
- `GET /recording/search?tpmql=...&start=N&limit=N` → TPMql-filtered list of stored recordings, renders `recording/index.jte`.
- `GET /recording/search/{id}` → single recording item detail, renders `recording/single.jte`.
- Pagination and filtering is done server-side via the `SimpleParser` TPMql engine.

#### `StorageHtmx`
- `GET /storage` → renders `storage.jte` (tree shell).
- `GET /storage/tree?parent=P&close=bool` → expandable directory tree node, renders `storage/tree.jte`.
- `GET /storage/files?parent=P` → file list for a directory, renders `storage/files.jte`.
- `GET /storage/file?parent=P` → single file content viewer, renders `storage/file.jte`.
- `POST /storage/file?parent=P` → create/update file content.
- `DELETE /storage/file?parent=P` → delete file.

### JTE Template Files

```
jte/
  index.jte             — Main page (root /)
  head.jte              — HTML <head> fragment (shared)
  header.jte            — Navigation header fragment
  footer.jte            — Footer fragment
  protocols.jte         — Protocol list
  plugins.jte           — Protocol plugin overview
  globalpl.jte          — Global plugins list
  recording.jte         — Recording search shell
  storage.jte           — Storage browser shell
  plugins/
    active.jte          — Active plugins widget
    wildcard.jte        — Wildcard plugin toggle
    protocol.jte        — Single protocol plugins
    single.jte          — Single protocol plugin detail+settings editor
    singlegl.jte        — Single global plugin detail
  recording/
    index.jte           — Filtered recording list table
    single.jte          — Single recording item detail
  storage/
    tree.jte            — Directory tree node
    files.jte           — File list
    file.jte            — File content viewer
  generic/
    protocol.jte        — Generic protocol rendering
    latency_plugin/index.jte
    mock_plugin/index.jte, single.jte
    network_error_plugin/index.jte
    record_plugin/index.jte
    replay_plugin/index.jte
    rest_plugins_plugin/index.jte
    rewrite_plugin/index.jte, single.jte
  global/
    report_plugin/
      index.jte         — Report plugin main view
      search.jte        — Report search/filter view
      html.jte          — HTML-format report output
```

---

## 5. REST API Layer

### `MainWebSite`
Extends `StaticWebFilter`, serves all static assets under the classpath path `*web` (Swagger UI, images, JavaScript, CSS).

### `ApiHandler`
Core REST API for protocol and global management. All routes under `/api/`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/protocols` | List all running protocol instances (`ProtocolIndex[]`) |
| GET | `/api/status` | Health check, returns `Ok` |
| GET | `/api/version` | Versions of runner + all PF4J plugins (`StringKvp[]`) |
| GET | `/api/global/settings` | Dump full `GlobalSettings` as JSON |
| GET | `/api/global/plugins` | List global plugins with active flag (`PluginIndex[]`) |
| GET | `/api/protocols/{id}/plugins` | List plugins for a protocol instance |
| POST | `/api/protocols/{instanceId}` | Patch protocol settings, write `settings.json`, trigger reload |
| GET | `/api/global/restart` | Trigger `RestartEvent` → restart server |
| GET | `/api/global/terminate` | Trigger `TerminateEvent` → shutdown |
| GET | `/api/protocols/all/plugins/{plugin}/{action}` | Bulk start/stop a plugin across all protocols |

The `POST /api/protocols/{instanceId}` route merges the incoming JSON patch into the serialized settings, writes `settings.json` to disk, writes to storage, and sends a `StorageReloadedEvent("settings.json")` which causes `Main` to restart with the updated file.

### `ApiStorageOnlyHandler`
Manages recorded scenario data:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/global/storage` | Download all storage as `storage.zip` |
| POST | `/api/global/storage` | Upload a ZIP to replace all storage |
| DELETE | `/api/global/storage` | Wipe all storage |
| GET | `/api/global/storage/index` | Paginated, TPMql-filterable list of `CompactLineApi[]` |
| GET | `/api/global/storage/item` | List all items with full `StorageAndIndex[]` (index + payload) |
| GET | `/api/global/storage/item/{protocol}/{index}` | Single item |
| PUT | `/api/global/storage/items/{protocol}/{index}` | Update a single item |
| DELETE | `/api/global/storage/items/{protocol}/{index}` | Delete a single item |

The `tpmql` query parameter accepts the TPMql expression language (a simple filter/select DSL) to filter index or item lists server-side.

### `SwaggerApi`
Generates OpenAPI 3.0 JSON dynamically at `GET /api/swagger/map.json`. It:
1. Iterates over all registered `FilterDescriptor` objects in `FiltersConfiguration.filters`.
2. For each annotated with `@TpmDoc`, builds an OpenAPI `PathItem` with the correct HTTP method, path/query/header parameters, request body, and response schemas.
3. Uses `ModelConverters` (swagger-core) to resolve Java classes into JSON Schema `$ref` entries.
4. Supports `@SwaggerEnricher` extensions (from plugins) to add extra paths/components.
5. The Swagger UI is served as a static asset, pointing to `/api/swagger/map.json`.

### Annotation Model for APIs

Every handler class and method uses:
- `@HttpTypeFilter` — marks a class as a filter (route group); `blocking=true` means the filter chain stops here on match.
- `@HttpMethodFilter(pathAddress, method, id)` — maps a method to a route. Supports `{paramName}` path variables.
- `@TpmDoc` — adds OpenAPI documentation metadata (description, request/response types, tags, query/path/header params, examples).

---

## 6. Plugin System

### Architecture

TPM has two plugin tiers:

1. **PF4J External Plugins** — JAR files in `pluginsDir`, loaded via `JarPluginManager`. Each must implement `ExtensionPoint`. Their `ExtensionPoint` subclasses are scanned and registered into the `DiService`. A `TPMPluginsClassLoader` unifies all plugin class loaders so DI resolution works across boundaries.

2. **Protocol Plugins** (`ProtocolPluginDescriptor`) — bound to a specific protocol instance; e.g. `record-plugin`, `replay-plugin`, `mock-plugin`. Configured per-protocol in settings. Each can expose API endpoints via `getApiHandler()`.

3. **Global Plugins** (`GlobalPluginDescriptor`) — cross-cutting concerns not tied to a single protocol. Configured at the global level in settings.

### `GlobalReportPlugin`

The only `GlobalPluginDescriptor` implemented in this module:

- **ID**: `report-plugin`
- **Settings class**: `PluginSettings` (base class; `active` flag only).
- **Registration**: On `initialize()`, subscribes to the `EventsQueue` for: `ReportDataEvent`, `RecordStatusEvent`, `ReplayStatusEvent`, `StorageReloadedEvent`, `SSLAddHostEvent`, `TerminateEvent`.
- **Event handling**: Uses a single-threaded `ExecutorService` to serialize all event writes. Each event is written to a `PluginFileManager` store (`global/report-plugin/`) as `0000000NNN.report` JSON files (zero-padded 10-digit counter). An in-memory `Map<String, Long> counters` accumulates numeric tag values (tags whose key starts with `@`).
- **`getReport()`** — re-reads all `.report` files from storage and returns a `GlobalReport` (event list + counters). This is intentionally lazy/persistent: events survive restarts.
- **`clear()`** — deletes all `.report` files.
- **Note**: `setActive(boolean)` always sets `this.active = true` regardless of the argument — the plugin cannot be deactivated after initialization.

### `GlobalReportPluginApiHandler`

API handler registered by `GlobalReportPlugin`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/global/plugins/report-plugin/{action}` | `start`, `stop`, `status`, `download`, `clean` |
| GET | `/api/global/plugins/report-plugin/report` | TPMql-filtered report data; formats: `json`, `csv`, `html` |
| DELETE | `/api/global/plugins/report-plugin/report` | Delete all stored report files |
| GET | `/global/plugins/report-plugin/report/search` | HTMX search fragment via `search.jte` |

The `loadReport` endpoint supports TPMql `select(...)` expressions for projections, plus `start`/`limit` pagination.

### `GlobalReportResult`

A simple DTO holding `ArrayNode rows` and `List<String> fields` (column names extracted from the first row). Used by both the JSON API and the JTE template renderer.

---

## 7. Storage API (`ApiStorageOnlyHandler`)

This handler is separated from `ApiHandler` precisely because it depends only on `StorageRepository`, not on live protocol instances. It provides the full CRUD surface for the recording store:

- **Download/upload ZIP** (`GET`/`POST /api/global/storage`): enables bulk export/import of recording scenarios. Upload triggers `storage.initialize()` to refresh in-memory indexes.
- **Index listing** (`GET /api/global/storage/index`): returns `CompactLineApi[]` (compact metadata) with optional TPMql filtering. Each item also gets a `fullItemAddress` URL pointing to its full item endpoint.
- **Item listing** (`GET /api/global/storage/item`): returns `StorageAndIndex[]` (full payload + index). Expensive — reads every item from storage.
- **Single item** (`GET /api/global/storage/item/{protocol}/{index}`): returns one `StorageAndIndex`.
- **Update** (`PUT /api/global/storage/items/{protocol}/{index}`): calls `storage.updateRecording(...)`.
- **Delete** (`DELETE /api/global/storage/items/{protocol}/{index}`): calls `storage.deleteRecording(...)`.

`CompactLineApi` extends `CompactLineComplete` (from the storage module) adding a `fullItemAddress` field for HATEOAS-style self-links.

`StorageAndIndex` bundles `StorageItem item` (the full request/response payload) with `CompactLineApi index` (metadata/summary).

---

## 8. `NotRespondingDecorator`

A Decorator pattern wrapper around `BaseApiServerHandler`. It overrides `respond()` with an **empty body** — i.e., it suppresses any HTTP response. All other methods (`isPartialPath`, `isPath`) delegate to the wrapped handler.

**Use case**: allows a handler to still perform path-matching (determining whether it owns a request) without actually sending a response. This is useful in plugin scenarios where a handler needs to "claim" a request for routing purposes but the actual response is deferred, suppressed, or handled by a different mechanism (e.g., protocol-level response injection).

---

## 9. `VersionChecker`

A minimal utility:

```java
public static String getTpmVersion() {
    return new String(
        VersionChecker.class.getResourceAsStream("/protocol_runner.version").readAllBytes()
    );
}
```

The file `/protocol_runner.version` is generated at build time by the `maven-antrun-plugin` (`setup-version` execution) during the `generate-resources` phase. It writes the Maven `${revision}` property directly into the classpath resource. Used by:

- `ProtocolsRunner.getMainOptions()` — printed in the help header.
- `ApiHandler.getVersion()` — returned in `GET /api/version` alongside PF4J plugin versions.
- `CommandOption.of("version")` — prints and exits when `-version` flag is passed.

---

## 10. Configuration (`settings.json` Format)

```json
{
  "pluginsDir":  "<path>",            // PF4J plugin JARs directory (default: "plugins")
  "logLevel":    "INFO|DEBUG|...",    // Logback level for org.kendar (default: "ERROR")
  "dataDir":     "<type>=<path>",     // e.g. "file=data", "encrypted=data"
  "apiPort":     5005,                // Management HTTP port (0 = disabled)
  "unattended":  false,               // Suppress Q/R stdin prompt

  "plugins": {                        // Global plugins section
    "<pluginId>": {
      "active": true                  // PluginSettings base fields
    }
  },

  "protocols": {                      // Map of named protocol instances
    "<instanceId>": {
      "protocol": "<protocolType>",   // e.g. "postgres", "http", "redis", "mqtt"
      "port": 5432,                   // Listen port for this protocol
      // ... protocol-specific fields ...
      "plugins": {
        "<pluginId>": {
          "active": true
          // ... plugin-specific settings ...
        }
      }
    }
  }
}
```

Environment variable `TPM_REPLACE=key1=val1,key2=val2` performs token substitution: `%key1%` in the JSON file is replaced with `val1` before parsing. This supports containerized/CI deployments without modifying config files.

---

## 11. Test Coverage Summary

The test suite uses JUnit 5 and Testcontainers. All integration tests inherit from `BasicTest` which provides a shared PostgreSQL `Testcontainers` instance and a `startAndHandleUnexpectedErrors(args)` helper that starts `Main.execute()` in a background thread and polls `Main.isRunning()`.

| Test Class | What It Tests |
|---|---|
| `BasicTest` | Shared base: Postgres container setup, proxy connection helper, server start helper |
| `ApiTestBase` | HTTP helper methods (GET/POST/download) for API tests |
| `StandardProtocolsTest` | End-to-end: Postgres proxy in forward mode, record+replay cycle, timeout handling, SQL error resilience |
| `HttpRunnerTest` | HTTP protocol in proxy mode: plain HTTP forward, HTTPS via SSL-bumping proxy, config-file driven start (`google.json`) |
| `MultiRecordReplayTest` | Multi-protocol scenario: simultaneous Postgres + HTTP + DNS recording via JSON config, then full replay; validates `GlobalReport` event counts via API |
| `ApiTest` | REST API: upload/download storage ZIP, HTTP plugin certificate API, static Swagger assets, protocol listing, bulk plugin start/stop |
| `ApiStorageTest` | Storage API: index listing count, single item retrieval by index |
| `HelpRunnerTest` | CLI help output: verifies `-help`, `-help http`, `-help mysql`, `-help postgres`, `-help amqp091`, `-help mqtt`, `-help redis`, `-help mongodb` all include the protocol name in output |
| `ApiEncryptedStorageTest` | Mirrors `ApiStorageTest` with an encrypted storage backend |
| `UiTest`, `UiSeleniumTest` | UI smoke tests via headless browser (Selenium) |
| `SeleniumTestBase` | Selenium test base infrastructure |
| `CompanyJpa` | JPA entity used by Postgres record/replay tests |
| `SimpleHttpServer` | Minimal embedded HTTP server used as test target for HTTP proxy tests |

---

## 12. Key Dependencies (`pom.xml`)

| Artifact | Purpose |
|---|---|
| `org.pf4j:pf4j` | Plugin framework — load JARs dynamically |
| `ch.qos.logback:logback-classic` | Logging (Logback backend) |
| `io.marioslab.basis:template:1.7` | JTE/Basis template engine for web UI |
| `com.jayway.jsonpath:json-path` | JSONPath support (used in query/filter) |
| `com.sun.net.httpserver` (JDK built-in) | Lightweight management API HTTP server |
| `io.swagger.v3` (transitive) | OpenAPI 3 model/serialization for SwaggerApi |
| `org.kendar.protocol:protocol-mongo` | MongoDB protocol implementation |
| `org.kendar.protocol:protocol-http` | HTTP/HTTPS proxy protocol |
| `org.kendar.protocol:protocol-postgres` | PostgreSQL wire protocol |
| `org.kendar.protocol:protocol-mysql` | MySQL wire protocol |
| `org.kendar.protocol:protocol-amqp-091` | AMQP 0.9.1 (RabbitMQ) protocol |
| `org.kendar.protocol:protocol-redis` | Redis protocol |
| `org.kendar.protocol:protocol-mqtt` | MQTT protocol |
| `org.kendar.protocol:protocol-test` | Test utilities (test scope) |
| `io.moquette:moquette-broker:0.15` | Embedded MQTT broker for tests (test scope) |
| `org.eclipse.paho:mqttv3:1.2.4` | MQTT client for tests (test scope) |

---

## 13. Data Flow Diagram (text)

```
CLI args / settings.json
        │
        ▼
  Main.execute()
        │
        ├─ DiService (IoC container)
        │        │
        │        ├─ registers: GlobalSettings, StorageRepository, FileResourcesUtils,
        │        │             FiltersConfiguration, PluginsLoggerFactory
        │        └─ discovers: ProtocolCommandLineHandler, PluginCommandLineHandler,
        │                      GlobalPluginDescriptor, NetworkProtoDescriptor, ...
        │
        ├─ JarPluginManager.loadPlugins()
        │        └─ ExtensionPoint classes → DiService.bind()
        │
        ├─ For each protocol in GlobalSettings.protocols [parallel threads]
        │        │
        │        ├─ Resolve NetworkProtoDescriptor by tag
        │        ├─ NettyServer.start()  ←── listens on TCP port
        │        ├─ ProtocolInstance created
        │        └─ ApiHandler.addProtocol(instance)
        │
        ├─ GlobalPluginDescriptor.initialize() [for each]
        │        └─ GlobalReportPlugin subscribes to EventsQueue
        │
        └─ HttpServer (com.sun.net) on apiPort
                 │
                 └─ ApiFiltersLoader (chain of FilteringClass)
                          │
                          ├─ MainHtmxPages        GET /
                          ├─ ProtocolsHtmx        GET /protocols[/...]
                          ├─ PluginsHtmx          GET /plugins[/...], /globalpl
                          ├─ RecordingHtmx        GET /recording[/...]
                          ├─ StorageHtmx          GET,POST,DELETE /storage[/...]
                          ├─ ApiHandler           GET,POST /api/protocols[/...]
                          │                       GET /api/global/[plugins|settings|restart|terminate]
                          ├─ ApiStorageOnlyHandler GET,POST,PUT,DELETE /api/global/storage[/...]
                          ├─ SwaggerApi           GET /api/swagger/map.json
                          ├─ GlobalReportPluginApiHandler
                          │                       GET,DELETE /api/global/plugins/report-plugin/...
                          └─ MainWebSite          GET /*web (static assets)

Inbound TCP traffic (protocol clients)
        │
        ▼
  NettyServer per protocol
        │
        └─ NetworkProtoDescriptor (state machine)
                 │
                 ├─ ProtocolPluginDescriptor chain (record, replay, mock, rewrite, …)
                 │
                 └─ StorageRepository (read/write CompactLine + StorageItem)
                          │
                          └─ file system or encrypted store under dataDir/

EventsQueue (in-process publish/subscribe)
  Publishers:  NettyServer, ProtocolPlugin, ApiHandler, Main
  Subscribers: Main (TerminateEvent, RestartEvent, StorageReloadedEvent),
               GlobalReportPlugin (ReportDataEvent, RecordStatus, ReplayStatus, ...)
```

---

## 14. Key Design Patterns Used

| Pattern | Where |
|---|---|
| **Dependency Injection (custom IoC)** | `DiService` — constructor injection, tag-based multi-binding, child scopes per thread. Eliminates Spring/CDI dependency. |
| **Plugin/Extension (PF4J)** | External JAR plugins loaded at runtime; `ExtensionPoint` sub-classes auto-registered into DI. |
| **Chain of Responsibility** | `ApiFiltersLoader` iterates `FilteringClass` implementations; first match wins (or `blocking=true` stops the chain). |
| **Decorator** | `NotRespondingDecorator` wraps `BaseApiServerHandler` to suppress response output while keeping path-matching. |
| **Observer / Event Bus** | `EventsQueue` (pub/sub) decouples lifecycle events (`TerminateEvent`, `RestartEvent`, `ReportDataEvent`, etc.) from producers. |
| **Factory Method** | `DiService.getInstance(X.class, "tag")` resolves tagged implementations (e.g. `NetworkProtoDescriptor` by protocol name). |
| **Command** | `CommandOption.withCallback(Consumer<String>)` — CLI option callbacks set settings fields imperatively. |
| **Template Method** | `BaseTemplate` — defines `loadTemplate`/`render`/`getTemplateContext`; subclasses fill in `getId()` and route handlers. |
| **Repository** | `StorageRepository` — abstract storage interface; concrete implementations (file, encrypted) resolved by DI tag. |
| **Facade** | `ApiHandler` provides a single API surface hiding the internal `ProtocolInstance`/`PluginDescriptor` graph. |
| **Thread-per-protocol startup** | Protocols start in parallel with a `CountDownLatch` to synchronize completion. |
| **Annotation-driven routing** | `@HttpTypeFilter`, `@HttpMethodFilter`, `@TpmDoc` annotations on handler methods are reflected at registration time to build both the filter chain and the OpenAPI spec. |
