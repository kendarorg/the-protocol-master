# protocol-common-jdbc Analysis

## Package Overview

| Package | Purpose |
|---|---|
| `org.kendar.sql.jdbc` | Core JDBC proxy: execution, result model, metadata, type conversion |
| `org.kendar.sql.jdbc.proxy` | JdbcCall DTO (in-flight call representation) |
| `org.kendar.sql.jdbc.storage` | JdbcRequest / JdbcResponse (serializable storage DTOs) |
| `org.kendar.sql.jdbc.utils` | DataTypesBuilder (type introspection from DatabaseMetaData) |
| `org.kendar.sql.parser` | SQL tokenizer and query-type classifier |
| `org.kendar.plugins` | JdbcRecordPlugin, JdbcReplayPlugin, JdbcMockPlugin, JdbcForwardMatcher |
| `org.kendar` | JdbcProtocol (protocol descriptor), JdbcJteResolver (template resolver) |

---

## 1. JdbcProxy — Core Execution

Central class: executes queries, reads ResultSet, converts to `SelectResult`.

### Binary column detection

`identifyFields()` sets `isByteData` per column using only:

```java
isByteOut(resultSetMetaData.getColumnClassName(i + 1))
```

`isByteOut` checks the last segment of the class name after splitting on `.`:

```java
return switch (name) {
    case ("[b"), ("[c"), ("byte") -> true;
    default -> false;
};
```

Java array class names are `[B` (byte[]) and `[C` (char[]). After `toLowerCase()`, `[B` → `[b` and `[C` → `[c`, so these cases DO match. **However**, this relies entirely on the JDBC driver returning `[B` as the column class name. Some drivers return `java.lang.String` for BINARY/VARBINARY columns — in that case `isByteData` is set to `false` and binary data is read via `getString()` instead of `getBytes()` → garbage or loss.

**The JDBC column type code (`getColumnType()`) is never consulted for this decision.** A robust fix would also check `Types.BINARY`, `Types.VARBINARY`, `Types.LONGVARBINARY`, `Types.BLOB`.

### Binary data round-trip

| Step | Action |
|---|---|
| Read | `rs.getBytes(i)` if `isByteData`, else `rs.getString(i)` |
| Store | `Base64.getEncoder().encodeToString(data)` → stored as String in `SelectResult` |
| Return to caller | Base64-encoded String as-is |

The caller (e.g., MySQL executor) is responsible for Base64-decoding before sending to the client. This works only if `isByteData` was correctly set.

### Parameter binding

`convertObject()` maps `BindingParameter.type` to Java types:

```java
case BINARY, BLOB, VARBINARY, LONGVARBINARY -> Base64.getDecoder().decode(val);
```

Binary params arriving from the protocol layer are expected to already be Base64-encoded strings. Decoded bytes are passed to `ps.setObject()`.

### Null binary handling

`toBytes()` returns `new byte[0]` for a null column value:

```java
if (rs.getBytes(i) == null) return new byte[]{};  // implicit via toBytes
```

`Base64.getEncoder().encodeToString(new byte[0])` = `""`. On replay, an empty string is indistinguishable from a null. **NULL binary columns may be replayed as empty byte arrays.**

---

## 2. SelectResult / ProxyMetadata

- `SelectResult`: holds rows as `List<List<String>>` — all values (including binary) are strings
- `ProxyMetadata`: per-column descriptor with `byteData` flag, `columnType` (JDBCType), `precision`
- `SelectResult.copy()` deep-copies metadata via `ProxyMetadata.copy()` — safe

---

## 3. BindingParameter

Stores a query parameter as a String value plus:
- `type` (JDBCType) — determines how the String is decoded back for `ps.setObject()`
- `binary` flag — set when the parameter originates from a binary binding
- `output` flag — for CallableStatement OUT parameters

**Concern:** Output parameters default to `VARBINARY` if `isBinary`, else `VARCHAR` (line 81 of JdbcProxy). No richer type info is preserved for the actual output type.

---

## 4. DataTypesBuilder / DataTypesConverter / DataTypeDescriptor

- `DataTypesBuilder` queries `DatabaseMetaData.getTypeInfo()` to build a mapping of native type OIDs → JDBC types
- `DataTypesConverter` holds bidirectional maps: native OID ↔ JDBCType
- Maps `BLOB` → `java.sql.Blob` (interface), not `byte[]`; this may cause `isByteOut` to return `false` for Blob columns depending on driver

---

## 5. SQL Parser (SqlStringParser)

A tokenizer, not a semantic SQL parser. Splits query text into typed tokens.

### Token types

| TokenType | Content |
|---|---|
| `VALUE_ITEM` | String literals (`'...'`, `"..."`), numbers |
| `QUERY_PARAM` | Parameter placeholders (configurable separator, default `$`) |
| `BLOB` | Everything else: keywords, identifiers, operators — **misnomer** |
| `COMMENT` | `--`, `/* */`, `#` comments |
| `SINGLE_ITEM` | Single special characters |

### What it handles

- Single-quoted strings with doubled-quote and backslash escaping
- Double-quoted identifiers
- Backtick-quoted identifiers (MySQL)
- `--` line comments, `/* */` block comments, `#` MySQL comments
- Multi-statement splitting on `;` (disabled inside PROCEDURE/FUNCTION bodies)
- Query type classification: SELECT, INSERT, UPDATE, DELETE, CALL, UNKNOWN

### What it does NOT handle

| Unsupported | Effect |
|---|---|
| Hex binary literals `X'ABCD'` / `0x...` | Passed as BLOB tokens; not detected as binary |
| PostgreSQL dollar-quoted strings `$$..$$` | Treated as parameter placeholders |
| CTE / WITH clauses | Type classified as UNKNOWN |
| Nested subqueries | Treated as single statement |
| Named parameters `@name` (SQL Server) | Not recognized |
| Multi-line string literals | Handled if no embedded newline breaks quoting |

---

## 6. Storage DTOs (JdbcRequest / JdbcResponse)

### JdbcRequest

Built from `JdbcCall` + List of `BindingParameter`. `textify()` strips output parameter values before storage (sets them null). Binary parameters are already Base64-encoded strings at this point.

**Redundant code:** `pv.setValue(pv.getValue())` for non-binary params is a no-op (value is already a String).

### JdbcResponse

Wraps either a `SelectResult` (for queries) or an integer count (for DML). No additional encoding — `SelectResult.records` already contains Base64 strings for binary columns.

---

## 7. Plugins

### JdbcRecordPlugin

- Intercepts `POST_CALL`; builds `JdbcRequest` + `JdbcResponse` and emits `WriteItemEvent`
- Skips `SET ...` statements (driver configuration noise)
- Tags: uses `buildTag(query, parser)` which tokenizes the query and strips literals → structural fingerprint

### JdbcReplayPlugin

- Intercepts `PRE_CALL`; looks up recorded response by `(caller, type, tags)` match
- **Parameter values are NOT matched** — only parameter count. Two calls with same SQL but different param values are treated as identical. First matching recording wins.
- Returns stored `SelectResult` directly (Base64 values pass through unchanged)

### JdbcMockPlugin

Fuzzy matching with scoring:

| Condition | Score |
|---|---|
| Exact parameter value match | +3 |
| Both null | +3 |
| Template `${...}` | +1 |
| Regex `@{...}` match | +2 |
| Exact query match | +10000 |
| Fuzzy query with templates | +1000 |

Applies template substitution to result records before returning. Supports `${varName}` extraction from parameters into result fields.

### JdbcForwardMatcher

Rewrites JDBC connection strings using regex groups. Used by BasicForwardPlugin to redirect connections (e.g., localhost → remote DB).

---

## 8. JdbcProtocol

Sets `isLateConnect() = true`: the physical DB connection is opened on the first query, not at the handshake. This avoids holding an idle DB connection during client authentication.

---

## 9. Critical Issues

| Issue | Location | Severity |
|---|---|---|
| Binary detection ignores JDBC type codes | `JdbcProxy.identifyFields()` | **HIGH** — drivers returning `java.lang.String` class name for BINARY/VARBINARY columns silently read binary data as text |
| NULL binary → empty byte[] conflation | `JdbcProxy.toBytes()` | **MEDIUM** — NULL columns replayed as empty arrays |
| Replay ignores parameter values | `JdbcReplayPlugin` | **MEDIUM** — wrong response returned when same query is called with different params |
| `BLOB` class → `java.sql.Blob`, not `[B` | `DataTypesBuilder` | **MEDIUM** — Blob columns fail `isByteOut` check |
| UTF-8 assumed for non-binary text | `JdbcProxy.toBytes()` | **LOW** — DB with non-UTF8 default charset corrupts text data |
| `textify()` no-op assignment | `JdbcRequest` | **LOW** — dead code, cosmetic |
| `TokenType.BLOB` misnomer | `SqlStringParser` | **LOW** — misleading but harmless |

---

## 10. Binary Data Full Path Summary

```
Real DB → ResultSet
  ↓ identifyFields()     [isByteData = isByteOut(className)]  ← MAY MISS SOME BINARY COLS
  ↓ iterateThroughRecSet()
      if isByteData → rs.getBytes() → Base64.encode() → String
      else          → rs.getString()                  → String
  ↓ SelectResult (List<List<String>>)
  ↓ JdbcRecordPlugin → JdbcResponse (stored as JSON)

Replay:
  ↓ JdbcReplayPlugin → load JdbcResponse
  ↓ return SelectResult unchanged (Base64 strings intact)
  ↓ Caller (e.g. MySQLExecutor / DataRow) decodes Base64 → raw bytes → client
```

The Base64 encoding boundary is at `JdbcProxy` (inbound from DB). Everything above it (plugins, storage, protocol executors) treats binary data as opaque Base64 strings. The final decode back to bytes happens in the protocol-specific layer (`DataRow`, `BinaryDataRow` in protocol-mysql).
