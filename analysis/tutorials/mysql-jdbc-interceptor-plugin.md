# Plan: MySQL JDBC Interceptor Plugin

Build a PF4J external plugin that attaches to the `mysql` protocol and intercepts every JDBC call flowing through the proxy. The plugin drops into `pluginsDir` as a JAR — no changes to core modules needed.

---

## Plugin Hook Points

Every JDBC call through the MySQL proxy passes through the plugin chain at two phases:

```
JDBC client  →  TPM proxy (port 3306)
                    │
                    ▼
              ┌─── PRE_CALL phase ──────────────────────────────────────┐
              │  plugin.handle(ctx, PRE_CALL, JdbcCall in, SelectResult) │
              │  return true  → short-circuit: skip proxy, use out as-is │
              │  return false → continue to real MySQL                   │
              └─────────────────────────────────────────────────────────┘
                    │
                    ▼
              Real MySQL server  (JdbcProxy.executeQuery / prepareStatement)
                    │
                    ▼
              ┌─── POST_CALL phase ─────────────────────────────────────┐
              │  plugin.handle(ctx, POST_CALL, JdbcCall in, SelectResult) │
              │  SelectResult is now populated — mutate or observe it    │
              │  return value ignored in POST_CALL                       │
              └─────────────────────────────────────────────────────────┘
                    │
                    ▼
              JDBC client (receives result)
```

Key types:
- `JdbcCall` — `getQuery(): String`, `getParameterValues(): List<BindingParameter>`
- `BindingParameter` — `getValue(): String`, `getType(): JDBCType`, `isBinary()`, `isOutput()`
- `SelectResult` — `getRecords(): List<List<String>>`, `getMetadata(): List<ProxyMetadata>`, `isIntResult()`, `getCount()`
- `PluginContext` — `getContextId()`, `getStart()` (millis), `getTags()`, `getCaller()`, `getType()`

---

## Module Layout

```
mysql-jdbc-interceptor/                    ← standalone Maven module
  pom.xml
  src/
    main/java/org/kendar/mysql/interceptor/
      MysqlInterceptorPlugin.java          ← PF4J Plugin entry point
      plugins/
        QueryLoggerPlugin.java             ← example 1: log every query
        QueryLoggerSettings.java
        QueryRewriterPlugin.java           ← example 2: rewrite SQL patterns
        QueryRewriterSettings.java
        QueryRewriterRule.java             ← single find→replace rule
        ResultMaskerPlugin.java            ← example 3: mask column values
        ResultMaskerSettings.java
        ResultMaskerRule.java              ← column name + masking pattern
    resources/
      mysql_interceptor.version            ← filled at build time by antrun
```

Alternatively, add to the existing `sample-plugins` module (simpler, already wired into the build).

---

## Step 1 — `pom.xml`

```xml
<parent>
    <groupId>org.kendar.protocol</groupId>
    <artifactId>the-protocol-master</artifactId>
    <version>${revision}</version>
</parent>
<artifactId>mysql-jdbc-interceptor</artifactId>
<packaging>jar</packaging>

<dependencies>
    <!-- compile-time: protocol abstractions only -->
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-common</artifactId>
        <version>${revision}</version>
        <scope>provided</scope>    <!-- runner already has this on classpath -->
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-common-jdbc</artifactId>
        <version>${revision}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-mysql</artifactId>
        <version>${revision}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.pf4j:pf4j</groupId>
        <version>${pf4j.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- test -->
    <dependency>
        <groupId>org.kendar.protocol</groupId>
        <artifactId>protocol-test</artifactId>
        <version>${revision}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers:mysql</groupId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Generate version resource (same pattern as other modules) -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-antrun-plugin</artifactId>
            <executions>
                <execution>
                    <id>setup-version</id>
                    <phase>generate-resources</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <echo file="${project.build.outputDirectory}/mysql_interceptor.version"
                                  message="${revision}"/>
                        </target>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        <!-- Fat JAR with all non-provided deps bundled -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals><goal>shade</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

All `protocol-*` deps are `provided` — they are already on the runner's classpath. Only bundle truly new transitive dependencies in the shaded JAR.

---

## Step 2 — Plugin Entry Point (`MysqlInterceptorPlugin.java`)

```java
public class MysqlInterceptorPlugin extends Plugin implements TPMPluginFile {

    @Override
    public void start() { }

    @Override
    public void stop() { }

    @Override
    public void delete() { }

    @Override
    public String getTpmPluginName() { return "mysql-jdbc-interceptor"; }

    @Override
    public String getTpmPluginVersion() {
        try {
            return new String(
                getClass().getResourceAsStream("/mysql_interceptor.version").readAllBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

PF4J discovers `MysqlInterceptorPlugin` via `META-INF/MANIFEST.MF`:
```
Plugin-Class: org.kendar.mysql.interceptor.MysqlInterceptorPlugin
Plugin-Id: mysql-jdbc-interceptor
Plugin-Version: ${revision}
```

---

## Step 3 — Example 1: Query Logger (`QueryLoggerPlugin.java`)

Logs every SQL statement with execution time. No short-circuiting.

```java
@Extension
@TpmService(tags = "mysql")
public class QueryLoggerPlugin
        extends ProtocolPluginDescriptorBase<QueryLoggerSettings> {

    private static final Logger log = LoggerFactory.getLogger(QueryLoggerPlugin.class);

    public QueryLoggerPlugin(JsonMapper mapper) { super(mapper); }

    @Override public String getId()       { return "mysql-query-logger"; }
    @Override public String getProtocol() { return "mysql"; }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL, ProtocolPhase.POST_CALL);
    }

    @Override
    public Class<?> getSettingClass() { return QueryLoggerSettings.class; }

    // PRE_CALL: record start time in PluginContext tags
    // POST_CALL: log query + duration + row count
    public boolean handle(PluginContext ctx, ProtocolPhase phase,
                          JdbcCall in, SelectResult out) {
        if (!isActive()) return false;

        if (phase == ProtocolPhase.PRE_CALL) {
            // nothing to short-circuit; just observe
            return false;
        }

        // POST_CALL
        var settings = (QueryLoggerSettings) getSettings();
        var duration  = System.currentTimeMillis() - ctx.getStart();
        var query     = in.getQuery();
        var params    = in.getParameterValues().size();
        var rows      = out.isIntResult()
                        ? "affected=" + out.getCount()
                        : "rows=" + out.getRecords().size();

        if (settings.isSlowQueryOnly() && duration < settings.getSlowThresholdMs()) {
            return false;
        }

        log.info("[MYSQL][{}ms] {} params={} {} | {}",
                 duration, query, params, rows, ctx.getContextId());

        if (settings.isLogParameters()) {
            for (int i = 0; i < in.getParameterValues().size(); i++) {
                var p = in.getParameterValues().get(i);
                log.info("  param[{}] type={} value={} binary={}",
                         i, p.getType(), p.getValue(), p.isBinary());
            }
        }
        return false;
    }

    @Override public Class<?> getInputType()  { return JdbcCall.class; }
    @Override public Class<?> getOutputType() { return SelectResult.class; }
}
```

### `QueryLoggerSettings.java`

```java
public class QueryLoggerSettings extends PluginSettings {
    private boolean logParameters   = false;
    private boolean slowQueryOnly   = false;
    private long    slowThresholdMs = 500;
    // getters + setters
}
```

`settings.json`:
```json
"mysql-query-logger": {
    "active": true,
    "logParameters": true,
    "slowQueryOnly": false,
    "slowThresholdMs": 200
}
```

---

## Step 4 — Example 2: Query Rewriter (`QueryRewriterPlugin.java`)

Rewrites SQL patterns in `PRE_CALL` before the query reaches MySQL. Returns `false` — the proxy still executes the rewritten query.

```java
@Extension
@TpmService(tags = "mysql")
public class QueryRewriterPlugin
        extends ProtocolPluginDescriptorBase<QueryRewriterSettings> {

    public QueryRewriterPlugin(JsonMapper mapper) { super(mapper); }

    @Override public String getId()       { return "mysql-query-rewriter"; }
    @Override public String getProtocol() { return "mysql"; }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public Class<?> getSettingClass() { return QueryRewriterSettings.class; }

    public boolean handle(PluginContext ctx, ProtocolPhase phase,
                          JdbcCall in, SelectResult out) {
        if (!isActive()) return false;
        var settings = (QueryRewriterSettings) getSettings();
        var query = in.getQuery();

        for (var rule : settings.getRules()) {
            if (rule.isRegex()) {
                query = query.replaceAll(rule.getFind(), rule.getReplace());
            } else {
                query = query.replace(rule.getFind(), rule.getReplace());
            }
        }
        in.setQuery(query);
        return false; // always continue to real MySQL
    }

    @Override public Class<?> getInputType()  { return JdbcCall.class; }
    @Override public Class<?> getOutputType() { return SelectResult.class; }
}
```

### `QueryRewriterSettings.java`

```java
public class QueryRewriterSettings extends PluginSettings {
    private List<QueryRewriterRule> rules = new ArrayList<>();
    // getters + setters
}
```

### `QueryRewriterRule.java`

```java
public class QueryRewriterRule {
    private String  find;
    private String  replace;
    private boolean regex = false;
    // getters + setters
}
```

`settings.json`:
```json
"mysql-query-rewriter": {
    "active": true,
    "rules": [
        { "find": "old_table", "replace": "new_table", "regex": false },
        { "find": "LIMIT \\d+", "replace": "LIMIT 100", "regex": true }
    ]
}
```

---

## Step 5 — Example 3: Result Masker (`ResultMaskerPlugin.java`)

Masks column values in `POST_CALL`. Useful for PII in test environments.

```java
@Extension
@TpmService(tags = "mysql")
public class ResultMaskerPlugin
        extends ProtocolPluginDescriptorBase<ResultMaskerSettings> {

    public ResultMaskerPlugin(JsonMapper mapper) { super(mapper); }

    @Override public String getId()       { return "mysql-result-masker"; }
    @Override public String getProtocol() { return "mysql"; }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL);
    }

    @Override
    public Class<?> getSettingClass() { return ResultMaskerSettings.class; }

    public boolean handle(PluginContext ctx, ProtocolPhase phase,
                          JdbcCall in, SelectResult out) {
        if (!isActive()) return false;
        if (out.isIntResult()) return false;

        var settings = (ResultMaskerSettings) getSettings();
        var metadata = out.getMetadata();

        // Build index of columns to mask
        var maskIndices = new HashMap<Integer, ResultMaskerRule>();
        for (int col = 0; col < metadata.size(); col++) {
            var colName = metadata.get(col).getColumnName();
            for (var rule : settings.getRules()) {
                if (rule.isRegex()
                        ? colName.matches(rule.getColumnPattern())
                        : colName.equalsIgnoreCase(rule.getColumnPattern())) {
                    maskIndices.put(col, rule);
                }
            }
        }
        if (maskIndices.isEmpty()) return false;

        // Mask matching column values in every row
        for (var row : out.getRecords()) {
            for (var entry : maskIndices.entrySet()) {
                int idx  = entry.getKey();
                var rule = entry.getValue();
                if (idx < row.size() && row.get(idx) != null) {
                    row.set(idx, applyMask(row.get(idx), rule));
                }
            }
        }
        return false;
    }

    private String applyMask(String value, ResultMaskerRule rule) {
        return switch (rule.getMaskType()) {
            case FULL    -> rule.getMaskChar().repeat(value.length());
            case PARTIAL -> {
                int keep = Math.min(rule.getKeepChars(), value.length());
                yield value.substring(0, keep) +
                      rule.getMaskChar().repeat(value.length() - keep);
            }
            case REGEX   -> value.replaceAll(rule.getMaskPattern(), rule.getMaskChar());
            case STATIC  -> rule.getMaskChar();
        };
    }

    @Override public Class<?> getInputType()  { return JdbcCall.class; }
    @Override public Class<?> getOutputType() { return SelectResult.class; }
}
```

### `ResultMaskerSettings.java`

```java
public class ResultMaskerSettings extends PluginSettings {
    private List<ResultMaskerRule> rules = new ArrayList<>();
}
```

### `ResultMaskerRule.java`

```java
public class ResultMaskerRule {
    private String   columnPattern;         // column name or regex
    private boolean  regex         = false;
    private MaskType maskType      = MaskType.FULL;
    private String   maskChar      = "*";
    private int      keepChars     = 4;     // for PARTIAL
    private String   maskPattern;           // for REGEX mask type
    // getters + setters

    public enum MaskType { FULL, PARTIAL, REGEX, STATIC }
}
```

`settings.json`:
```json
"mysql-result-masker": {
    "active": true,
    "rules": [
        { "columnPattern": "email",        "maskType": "PARTIAL", "keepChars": 3 },
        { "columnPattern": "credit_card",  "maskType": "FULL",    "maskChar": "X" },
        { "columnPattern": ".*_secret$",   "regex": true, "maskType": "STATIC", "maskChar": "[REDACTED]" }
    ]
}
```

---

## Step 6 — Short-Circuiting (Mock Responses)

Return `true` from `PRE_CALL` to skip the real MySQL entirely. Populate `out` before returning:

```java
public boolean handle(PluginContext ctx, ProtocolPhase phase,
                      JdbcCall in, SelectResult out) {
    if (phase != ProtocolPhase.PRE_CALL) return false;
    if (!in.getQuery().toUpperCase().startsWith("SELECT 1")) return false;

    // Fake result: single row, single column
    out.getMetadata().add(new ProxyMetadata("1", false, Types.INTEGER, 10));
    out.getRecords().add(List.of("1"));
    return true; // ← SHORT-CIRCUIT: real MySQL not called
}
```

**Warning**: `out` is only valid to populate when `phase == PRE_CALL`. In `POST_CALL` it already contains the real result — replace it by calling `out.fill(newResult)` rather than adding to the existing collections.

---

## Step 7 — Accessing Binding Parameters

For prepared statements (`COM_STMT_EXECUTE`), values arrive as `BindingParameter`:

```java
for (var param : in.getParameterValues()) {
    if (param.isOutput()) continue;             // OUT/INOUT param, no input value
    if (param.isBinary()) {
        // binary data: Base64-encoded in getValue()
        byte[] raw = Base64.getDecoder().decode(param.getValue());
        // ...
    } else {
        String text = param.getValue();         // null-safe: can be null for SQL NULL
        JDBCType type = param.getType();        // INTEGER, VARCHAR, TIMESTAMP, ...
    }
}
```

To replace a parameter value (e.g. override a date argument):
```java
param.setValue("2024-01-01");
// Type stays the same; MySQL proxy re-binds it
```

---

## Step 8 — Accessing Context and Tags

`PluginContext` carries per-call metadata useful for routing logic:

```java
var contextId = ctx.getContextId();   // unique per TCP connection
var caller    = ctx.getCaller();      // "MYSQL"
var type      = ctx.getType();        // "COM_QUERY" or "COM_STMT_EXECUTE"
var start     = ctx.getStart();       // System.currentTimeMillis() when call started
var tags      = ctx.getTags();        // Map<String, Object>; "id" key = storage sequence id
```

Store cross-phase state in tags (PRE sets, POST reads):
```java
// PRE_CALL
ctx.getTags().put("originalQuery", in.getQuery());

// POST_CALL
var original = (String) ctx.getTags().get("originalQuery");
```

---

## Step 9 — Testing

### Test base

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MysqlInterceptorPluginTest {

    private static MySQLContainer<?> mysql;

    @BeforeAll
    static void startInfra() {
        mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("testdb")
            .withUsername("root")
            .withPassword("root");
        mysql.start();
    }

    @BeforeEach
    void startProxy() throws Exception {
        Main.execute(new String[]{
            "-cfg", "src/test/resources/settings-test.json",
            "-un"
        });
        Awaitility.await().atMost(10, SECONDS).until(Main::isRunning);
    }

    @AfterEach
    void stopProxy() {
        Main.stop();
    }

    @AfterAll
    static void stopInfra() {
        mysql.stop();
    }

    protected Connection proxyConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3307/testdb", "root", "root");
    }
}
```

### Test: query logger writes to log

```java
@Test @Order(1)
void queryLogger_logsSlowQuery() throws Exception {
    // Use a ListAppender on the Logback logger
    var appender = attachListAppender(QueryLoggerPlugin.class);

    try (var conn = proxyConnection();
         var stmt = conn.createStatement()) {
        stmt.execute("SELECT SLEEP(0.3)");
    }

    var messages = appender.getEvents().stream()
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
    assertTrue(messages.stream().anyMatch(m -> m.contains("SELECT SLEEP")));
}
```

### Test: query rewriter changes table name

```java
@Test @Order(2)
void queryRewriter_replacesTableName() throws Exception {
    // Create both tables in MySQL container
    setupTable("old_table");
    setupTable("new_table");
    insertRow("new_table", "hello");

    try (var conn = proxyConnection();
         var stmt = conn.createStatement();
         var rs = stmt.executeQuery("SELECT * FROM old_table")) {
        // Rewriter rule: old_table → new_table
        assertTrue(rs.next());
        assertEquals("hello", rs.getString(1));
    }
}
```

### Test: result masker hides email

```java
@Test @Order(3)
void resultMasker_masksEmailColumn() throws Exception {
    insertUser("alice@example.com");

    try (var conn = proxyConnection();
         var stmt = conn.createStatement();
         var rs = stmt.executeQuery("SELECT email FROM users")) {
        assertTrue(rs.next());
        var masked = rs.getString("email");
        assertTrue(masked.startsWith("ali"));      // keepChars=3
        assertTrue(masked.endsWith("*".repeat(masked.length() - 3)));
    }
}
```

### Test settings (`src/test/resources/settings-test.json`)

```json
{
    "pluginsDir": "target/plugins",
    "logLevel":   "DEBUG",
    "dataDir":    "file=target/test-data",
    "apiPort":    5006,
    "protocols": {
        "mysql-01": {
            "protocol": "mysql",
            "port": 3307,
            "connectionString": "localhost:${MYSQL_PORT}",
            "login": "root",
            "password": "root",
            "plugins": {
                "mysql-query-logger":   { "active": true, "logParameters": true },
                "mysql-query-rewriter": { "active": true, "rules": [
                    { "find": "old_table", "replace": "new_table" }
                ]},
                "mysql-result-masker":  { "active": true, "rules": [
                    { "columnPattern": "email", "maskType": "PARTIAL", "keepChars": 3 }
                ]}
            }
        }
    }
}
```

Use `TPM_REPLACE=MYSQL_PORT=<port>` env var to inject the Testcontainers-assigned port at runtime.

---

## Step 10 — Deployment

1. Build: `mvn package -pl mysql-jdbc-interceptor`
2. Copy `target/mysql-jdbc-interceptor-${revision}.jar` into the runner's `pluginsDir` (e.g. `plugins/`).
3. Add plugin settings under the `mysql-XX` protocol block in `settings.json`.
4. Start or restart the runner — plugin JARs are loaded once at startup.

**Upgrade**: replace the JAR and restart. Settings persist in `settings.json` across restarts.

---

## Step 11 — Best Practices for MySQL JDBC Plugins

| Concern | Guidance |
|---|---|
| **Phase selection** | Use `PRE_CALL` only to inspect/rewrite input or short-circuit. Use `POST_CALL` to inspect/mutate results. Never add to `out.getRecords()` in `POST_CALL` after they are already filled — use `out.fill(copy)` to replace. |
| **Short-circuit safety** | When returning `true` from `PRE_CALL`, always populate `out.getMetadata()` to match `out.getRecords()`. A column count mismatch causes a JDBC `SQLException` on the client. |
| **Null parameters** | `BindingParameter.getValue()` can be `null` (SQL `NULL`). Always null-check before calling `.equalsIgnoreCase()` or pattern matching. |
| **PreparedStatement identity** | `ctx.getType()` = `"COM_STMT_EXECUTE"` for prepared statements. The query string in `JdbcCall` is the original prepared SQL (with `?` placeholders), not the interpolated form. |
| **Thread safety** | Plugin instances are singletons per protocol instance. Settings object is shared across all concurrent connections. If you mutate settings at runtime, use volatile fields or synchronization. |
| **Performance** | `POST_CALL` runs on the Netty I/O thread pool. Keep it fast — no blocking I/O, no heavy regex on large result sets. |
| **`isActive()` check** | Always guard with `if (!isActive()) return false;` as the first line. The runner can toggle plugins on/off at runtime via the API. |
| **Settings null safety** | `getSettings()` can return `null` if the plugin is active but its settings block is absent from `settings.json`. Cast defensively. |
| **`shouldIgnoreTrivialCalls()`** | If extending `JdbcRecordPlugin`, override `shouldNotSaveJdbc()` to skip housekeeping queries (`SET`, `SHOW`, `USE`). |

---

## Implementation Order

1. `pom.xml` + `META-INF/MANIFEST.MF` (PF4J wiring)
2. `MysqlInterceptorPlugin.java` + version resource
3. `QueryLoggerSettings` + `QueryLoggerPlugin` + logger test
4. `QueryRewriterRule` + `QueryRewriterSettings` + `QueryRewriterPlugin` + rewrite test
5. `ResultMaskerRule` + `ResultMaskerSettings` + `ResultMaskerPlugin` + masker test
6. Integration: drop JAR in `pluginsDir`, smoke-test via `GET /api/protocols/mysql-01/plugins`
7. (Optional) Add a REST API handler (`ProtocolPluginApiHandler`) for runtime rule CRUD via the management API
