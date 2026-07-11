# Plan: New `protocol-mssql` module (MS SQL Server / TDS over TCP)

## Context

The Protocol Master simulates/proxies/records network protocols (HTTP, Postgres, MySQL, MongoDB, Redis, MQTT, AMQP, DNS). The two SQL protocols (protocol-mysql, protocol-postgres) implement the native wire protocol toward clients and forward queries to a real database via the shared JDBC layer (`protocol-common-jdbc`: `JdbcProtocol`, `JdbcProxy`, `SelectResult`, `ProxyMetadata`, `BindingParameter`, and the shared `JdbcRecord/Replay/Mock/Rewrite` plugins). This plan adds a third SQL protocol: **MS SQL Server**, speaking the TDS (Tabular Data Stream, MS-TDS spec) protocol on port 1433, forwarding via the `com.microsoft.sqlserver.jdbc.SQLServerDriver` JDBC driver.

The module mirrors protocol-mysql/protocol-postgres structure exactly. TDS is message-based like Postgres, so the FSM design follows the Postgres pattern (a packet-translator interrupt state + per-message-type states) rather than MySQL's.

## Scope (Phase 1)

**In**: TLS support like MySQL (see TLS section below) plus plaintext fallback for `encrypt=false` clients, Login7 SQL auth (credentials accepted, proxy uses configured credentials — same as mysql/postgres), SQL Batch, RPC prepared statements (`sp_executesql`, `sp_prepare`, `sp_execute`, `sp_prepexec`, `sp_unprepare`), transactions (Transaction Manager requests 0x0E), Attention/cancel, core data types (int family, bit, float/real, decimal/numeric, [n]varchar/char, varbinary, date/time/datetime/datetime2, uniqueidentifier, money).

**Deferred (document in README "Missing features")**: ENCRYPT_OFF semantics (login-only encryption — we always answer full ON or NOT_SUP), MARS, bulk load (0x07), NTLM/Kerberos/FedAuth/Always-Encrypted, table-valued params, output params beyond the prepare handle, NBCROW, server cursors (sp_cursoropen → ERROR token so drivers fall back), XML/sql_variant.

## 1. Scaffolding (all verified locations)

- `protocol-mssql/pom.xml` — clone `protocol-mysql/pom.xml`: parent `org.kendar.protocol:protocol-master:4.3.10-tpm`, deps `protocol-common`, `protocol-common-jdbc`, pf4j, `com.microsoft.sqlserver:mssql-jdbc:${mssql.version}` (compile, needed at runtime for forwarding), test-scope `protocol-test`, jaxb-api, commons-lang3/io. Keep `maven.deploy.skip` pattern of siblings.
- Parent `pom.xml`: add `<mssql.version>12.8.1.jre11</mssql.version>` property; add `<module>protocol-mssql</module>` to **both** `dev` (~line 172) and `deploy` (~line 194) profiles.
- `protocol-runner/pom.xml`: add `protocol-mssql` dependency (next to protocol-mysql, ~line 76).
- `protocol-test/pom.xml`: add `org.testcontainers:mssqlserver:${testcontainers.version}`.
- `jacoco/pom.xml`: add the three `protocol-mssql` fileset/dirset entries (exec file, src dirset, classes fileset, ~lines 68–160).

## 2. New module contents (package `org.kendar.mssql`)

### Core classes (templates in parentheses)
- `MssqlProtocol extends JdbcProtocol` (`MySQLProtocol.java`, `PostgresProtocol.java`) — `@TpmService(tags="mssql")`; `@TpmConstructor(GlobalSettings, MssqlProtocolSettings, MssqlProxy, @TpmNamed(tags="mssql") List<BasePluginDescriptor>)` + `()` + `(int port)`; PORT=1433; `isBe()=true`; `createContext` → `MssqlProtoContext` with `MssqlExecutor` and `setValue("PARSER", new SqlStringParser("@"))`.
- `MssqlProtocolSettings extends JdbcProtocolSettings` — `setProtocol("mssql")`.
- `MssqlProxy extends JdbcProxy` — `getDefaultDriver()` → `"com.microsoft.sqlserver.jdbc.SQLServerDriver"`.
- `MssqlJteResolver extends JteResolver` + `resources/jte/mssql/protocol.jte` (copy/adapt `jte/mysql/protocol.jte` — only one template per module).
- `buffers/MssqlBBuffer extends BBuffer` (template `MySQLBBuffer`) — TDS payload fields are little-endian, strings UCS-2/UTF-16LE: `read/writeUShortLE`, `read/writeUIntLE`, `read/writeULongLE`, `readBVarchar/writeBVarchar` (1-byte char count + UTF-16LE), `readUsVarchar/writeUsVarchar` (2-byte), `writeCollation(byte[5])`.
- `executor/MssqlProtoContext extends NetworkProtoContext` — `buildBuffer` override → MssqlBBuffer; cancel flag (copy `PostgresProtoContext.cancel()`); prepared-handle map; negotiated packet size; txn descriptor; `runException` override → ERROR token + DONE(DONE_ERROR).
- `executor/MssqlExecutor` (template `PostgresExecutor.executePortal`) — fake-query `BasicHandler` list (`SET LOCK_TIMEOUT`, `SET TEXTSIZE`, `SET IMPLICIT_TRANSACTIONS`, `SELECT @@SPID`, driver bootstrap queries); `BEGIN/COMMIT/ROLLBACK TRAN` keywords → `JdbcProxy.executeBegin/Commit/Rollback`; else forward → token-stream messages.

### FSM (`fsm/`, templates: `PostgresPacketTranslator`, `PostgresState`)
TDS framing: 8-byte header — Type 1B, Status 1B (bit 0x01 = EOM), Length 2B **big-endian**, SPID 2B, PacketID 1B, Window 1B. Messages may span packets; reassemble centrally:
- `fsm/events/TdsPacket extends ProtocolEvent` — reassembled payload buffer + packet type byte.
- `TdsPacketTranslator extends ProtoState implements InterruptProtoState` — `canRun`: need ≥8 bytes; walk packets by BE length; incomplete → `AskMoreDataException`; complete when a packet has EOM. `execute`: strip headers, concatenate payloads, truncate input, `context.send(new TdsPacket(...))`.
- `TdsState extends ProtoState` — `canRun(TdsPacket)` matches `getPacketType()`; abstract `executeTds(MssqlBBuffer, MssqlProtoContext)`.
- States: `PreLogin` (0x12 → respond VERSION, ENCRYPTION per negotiation below, INSTOPT, THREADID, MARS=0, 0xFF terminator), `TdsSslHandshake` (optional, see TLS section), `Login7` (0x10 → parse LE fixed part + offset/length string table; store login/database; respond ENVCHANGE(1 database), ENVCHANGE(4 packet size), LOGINACK (TDS 7.4 = `0x74000004`, progname "Microsoft SQL Server"), DONE), `SqlBatch` (0x01 → skip ALL_HEADERS by its leading DWORD TotalLength, UCS-2LE SQL to end → executor), `Rpc` (0x03 → US_VARCHAR proc name or 0xFFFF + ProcID 10/11/12/13/15; parse params: B_VARCHAR name, status, TYPE_INFO, value; translate `@pN` via `SqlStringParser("@")` + `JdbcProxy.buildParametrizedStatement`; respond result tokens + RETURNVALUE (prepare handle) + RETURNSTATUS + DONEPROC), `TransactionManager` (0x0E → type 5 begin/7 commit/8 rollback + ENVCHANGE 8/9/10), `Attention` (0x06 interrupt state → cancel flag + DONE status 0x0020).

`initializeProtocol()`:
```java
addInterruptState(new TdsPacketTranslator(BytesEvent.class));
addInterruptState(new Attention(TdsPacket.class));
initialize(new ProtoStateSequence(
    new PreLogin(TdsPacket.class),
    new TdsSslHandshake(BytesEvent.class).asOptional(),
    new Login7(TdsPacket.class),
    new ProtoStateWhile(new ProtoStateSwitchCase(
        new SqlBatch(TdsPacket.class),
        new Rpc(TdsPacket.class),
        new TransactionManager(TdsPacket.class)))));
```

### TLS (like MySQL, honoring the inherited `useTls` setting)
The common machinery (`protocol-common/src/main/java/org/kendar/protocol/states/SSLHandshake.java`) works at the Netty pipeline level: it adds an `SslHandler` (from `NetworkProtoContext.getSslContext()`) and replays raw TLS record bytes into the pipeline via `fireChannelRead`. MySQL reuses it directly because after SSLRequest the client speaks raw TLS. **TDS differs in one way**: during negotiation the TLS handshake records travel *wrapped in TDS 0x12 PRELOGIN packets*; only after the handshake completes does traffic become raw TLS (which the in-pipeline `SslHandler` then handles transparently, including the encrypted Login7 and everything after).

Plan:
- `PreLogin` responds ENCRYPTION=`0x01` ENCRYPT_ON when `useTls` is set or the client requests encryption, else `0x02` ENCRYPT_NOT_SUP (plaintext path — clients with `encrypt=false` still work). PreLogin request/response themselves are plaintext.
- New `TdsSslHandshake` state (optional in the sequence, modeled on the common `SSLHandshake`): matches `TdsPacket` events of type 0x12 whose payload starts with a TLS record (0x16) while the SSL context flag is set. It feeds the **reassembled payload** (TDS headers already stripped by `TdsPacketTranslator`) into the pipeline's `SslHandler` via `fireChannelRead`, exactly like `SSLHandshake.execute` does.
- Outbound wrapping: while the handshake is in progress, the `SslHandler`'s handshake responses must be wrapped in TDS 0x12 packets. Add a small Netty outbound handler between the `SslHandler` and the socket that prepends the 8-byte TDS header (type 0x12, EOM) to outgoing TLS records; remove it in the `handshakeFuture` listener (same listener pattern as `SSLHandshake.initializeSslUpdate`). After removal, TLS flows raw and the rest of the FSM is unchanged — `TdsPacketTranslator` sees decrypted TDS bytes.
- Certificate/SslContext comes from the existing `NetworkProtoContext.getSslContext()` machinery — no new cert code; same configuration as MySQL's TLS.
- Test client side: mssql-jdbc with `encrypt=true;trustServerCertificate=true` (its default), plus a plaintext test with `encrypt=false`.

### Messages (`messages/`)
`TdsReturnMessage implements NetworkReturnMessage` base: buffers token bytes, `write()` splits into type-0x04 packets ≤ negotiated size (Status 0x01 on last). Tokens: `PreLoginResponse`, `ColMetadataToken` (0x81), `RowToken` (0xD1), `DoneToken` (0xFD; status flags FINAL 0x0/MORE 0x1/ERROR 0x2/COUNT 0x10/ATTN_ACK 0x20), `DoneProcToken` (0xFE), `DoneInProcToken` (0xFF), `ErrorToken` (0xAA), `InfoToken` (0xAB), `LoginAckToken` (0xAD), `EnvChangeToken` (0xE3), `ReturnStatusToken` (0x79), `ReturnValueToken` (0xAC).

### Constants (`constants/`)
`TdsPacketType`, `TdsTokenType`, `TdsDataType` (nullable-form types: INTN 0x26, BITN 0x68, FLTN 0x6D, DECIMALN 0x6A/NUMERICN 0x6C, NVARCHAR 0xE7, BIGVARCHR 0xA7, BIGVARBIN 0xA5, DATETIMN 0x6F, DATEN 0x28, TIMEN 0x29, DATETIME2N 0x2A, GUID 0x24, MONEYN 0x6E, fixed INT1/2/4/8...), `EnvChangeType`, `TmRequestType`, `DoneStatus`, `RpcProcId`.

### Type conversion
- `resources/mssqldtt.json` + static `DataTypesConverter` init (copy `PostgresProtocol`'s `postgresdtt.json` pattern).
- Since `SelectResult` rows are strings + `isByteData` flags, phase 1 emits values mostly as NVARCHAR/BIGVARBIN with correct COLMETADATA typing from `ProxyMetadata` (same simplification Postgres uses with text results); tighten per-type binary encoding incrementally. NVARCHAR collation: `09 04 D0 00 34` (Latin1_General).

### Plugins (`plugins/`, all `@TpmService(tags="mssql")`, thin wrappers like mysql's)
9 plugins: `MssqlRecordPlugin extends JdbcRecordPlugin` (parser `SqlStringParser("@")`, `getProtocol()="mssql"`), `MssqlReplayPlugin`, `MssqlMockPlugin`, `MssqlRewritePlugin`, `MssqlReportPlugin`, `MssqlNetErrorPlugin`, `MssqlLatencyPlugin`, `MssqlForwardPlugin`, `MssqlRestPluginsPlugin`.
8 CLI classes in `plugins/cli/` (mirror mysql, note existing typo pattern `...NetworErrorPluginCli`): Record, Replay, Mock, Rewrite, Report, Latency, NetworError, Forward.

### CLI
`cli/MssqlCommandLineHandler extends NetworkProtocolCommandLineHandler` — `getId()="mssql"`, default port `1433`, connection description `jdbc:sqlserver://localhost:1433;encrypt=false;databaseName=master`, `buildProtocolSettings()` → `MssqlProtocolSettings`, `-js/--schema` custom option like mysql.

## 3. Testing

- New `protocol-test/src/main/java/org/kendar/tests/testcontainer/images/MsSqlServerImage.java extends BaseImage<MsSqlServerImage, MSSQLServerContainer>` — `new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest")).acceptLicense()` (sets ACCEPT_EULA=Y, mandatory); jdbcUrl suffixed `;encrypt=false;trustServerCertificate=true`; init scripts via mybatis ScriptRunner (already a dep) with `IF DB_ID('x') IS NULL CREATE DATABASE x`.
- `MssqlBasicTest` cloned from `MySqlBasicTest` (FAKE_PORT 1435, `FileStorageRepository` under `target/tests`, plugin wiring, `Server`); clients connect via `jdbc:sqlserver://localhost:1435;encrypt=false;trustServerCertificate=true`.
- Tests: `MssqlProtocolTest` (SELECT/INSERT/UPDATE, error propagation), `MssqlSSLProtocolTest` (mirror `MySQLSSLProtocolTest`, client with `encrypt=true;trustServerCertificate=true`), `DataTypesTest`/`DataTypesPsTest`, `MssqlPrepStatementTest` (PreparedStatement → sp_prepexec path), transactions (autoCommit false/commit/rollback), `MockTest`, `ReplayerTest` with recorded scenario in `src/test/resources`. Copy `logback.xml`.

## 4. Docs

- `protocol-mssql/README.md` following `protocol-postgres/README.md` (config keys, plugin sections, "Missing features" listing deferred items, MS-TDS spec reference).
- Main `README.md`: add MSSQL to the supported-protocols list with link.

## 5. Verification

- `mvn -P dev -pl protocol-mssql -am compile`, then `mvn -P dev -pl protocol-mssql test` (Docker required).
- Full: `mvn -P dev install` from root.
- Manual: `java -jar protocol-runner/target/protocol-runner.jar -protocol mssql -port 1433 -connection "jdbc:sqlserver://REAL:1433;encrypt=false" -login sa -password ...`, connect with `sqlcmd -S localhost,1433 -U .. -P .. -C` or an mssql-jdbc client with `encrypt=false`.

## 6. Implementation order (milestones)

1. **Scaffold** — all pom changes + empty Protocol/Settings/Proxy/Context/Buffer/JteResolver + jte template → build green.
2. **Handshake** — TdsPacketTranslator, TdsPacket, TdsState, PreLogin, Login7, token base + LOGINACK/ENVCHANGE/DONE/ERROR → mssql-jdbc `getConnection` succeeds against the proxy.
3. **SQL Batch** — SqlBatch state + MssqlExecutor + COLMETADATA/ROW/DONE (NVARCHAR-everything encoding) → `SELECT 1` and DML rowcounts work.
4. **Data types** — mssqldtt.json, per-type encoding, DataTypesTest.
5. **RPC/prepared statements** — sp_executesql, sp_prepexec/sp_execute/sp_unprepare, RETURNSTATUS/RETURNVALUE, param decoding.
6. **Transactions + Attention.**
7. **TLS** — `TdsSslHandshake` state + outbound 0x12-wrapper handler; verify with mssql-jdbc default `encrypt=true`.
8. **Plugins + CLI** — 9 plugins, 8 plugin CLIs, MssqlCommandLineHandler (incl. `--useTls` option like mysql).
9. **Tests, MsSqlServerImage, READMEs, jacoco entries**, full `mvn -P dev install`.

## Key template files
- `protocol-postgres/src/main/java/org/kendar/postgres/PostgresProtocol.java` (descriptor + DataTypesConverter init)
- `protocol-postgres/src/main/java/org/kendar/postgres/fsm/PostgresPacketTranslator.java` (packet reassembly)
- `protocol-common-jdbc/src/main/java/org/kendar/sql/jdbc/JdbcProxy.java` (forwarding contract)
- `protocol-mysql/src/main/java/org/kendar/mysql/buffers/MySQLBBuffer.java` (LE buffer helpers)
- `protocol-mysql/src/main/java/org/kendar/mysql/plugins/` + `plugins/cli/` (plugin wrapper pattern)
- `protocol-mysql/src/test/java/org/kendar/mysql/MySqlBasicTest.java`, `protocol-test/src/main/java/org/kendar/tests/testcontainer/images/MysqlImage.java` (test pattern)
