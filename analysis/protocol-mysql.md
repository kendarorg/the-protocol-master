# protocol-mysql Analysis

## Package Overview

| Package | Purpose |
|---|---|
| `buffers` | `MySQLBBuffer` — MySQL-specific wire encoding on top of `BBuffer` |
| `constants` | Enums: `MySQLType`, `Language`, `CapabilityFlag`, `CommandType`, `StatusFlag`, `ErrorCode` |
| `executor` | `MySQLExecutor` (query dispatch/result building), `MySQLProtoContext` |
| `fsm` | FSM states: `ConnectionEstablished`, `Auth`, `Command`, `ComQuery`, `ComStmtPrepare`, `ComStmtExecute`, `ComPing`, `ComQuit`, `ComInitDb`, `ComRefresh`, `SSLRequest` |
| `fsm/events` | `CommandEvent` — carries command type + raw buffer through FSM |
| `messages` | Wire message serializers: `Handshake`, `OkPacket`, `EOFPacket`, `Error`, `ColumnDefinition`, `ColumnsCount`, `DataRow`, `BinaryDataRow`, `ComStmtPrepareOk`, `TextResultSet` |
| `plugins` | `MySqlForwardPlugin`, `MySqlRecordPlugin`, `MySqlReplayPlugin`, `MySqlMockPlugin`, etc. |

---

## 1. Wire Protocol Flow

```
Server → Client:  Handshake (ConnectionEstablished)
Client → Server:  [optional SSLRequest + TLS handshake]
Client → Server:  HandshakeResponse (Auth)
Server → Client:  OkPacket
─────────────── command loop ───────────────
Client → Server:  Command byte + payload (Command.java)
   COM_QUERY (0x03)          → ComQuery   → MySQLExecutor.executeText (text=true)
   COM_STMT_PREPARE (0x16)   → ComStmtPrepare → MySQLExecutor.prepareStatement
   COM_STMT_EXECUTE (0x17)   → ComStmtExecute → MySQLExecutor.executeText (text=false)
   COM_INIT_DB (0x02)        → ComInitDb
   COM_PING (0x0e)           → ComPing    → OkPacket
   COM_REFRESH (0x07)        → ComRefresh → OkPacket
   COM_QUIT (0x01)           → ComQuit    → connection close
```

**Missing command handlers** (no handler exists, client gets no response):
- `COM_STMT_CLOSE` (0x19) — prepared statement cleanup
- `COM_STMT_RESET` (0x1a)
- `COM_STMT_SEND_LONG_DATA` (0x18) — chunked BLOB upload
- `COM_STMT_FETCH` (0x1c) — cursor fetch
- `COM_CHANGE_USER` (0x11)

---

## 2. MySQLBBuffer

Extends `BBuffer`. MySQL protocol is **little-endian**; `MySQLBBuffer` defaults to `BBufferEndianness.BE` but all multi-byte integer operations use explicit LE implementations:

| Method | Encoding | Correct for MySQL? |
|---|---|---|
| `readUB2/3/4` | explicit LE | ✓ |
| `writeUB2/3/4` | explicit LE | ✓ |
| `getFloatLe` / `getDoubleLe` | LE via endianness swap if BE | ✓ |
| `writeFloatLe` / `writeDoubleLe` | LE via swap if BE | ✓ |
| `getLong` | reads raw bytes, LE reconstruction (no swap for BE) | ✓ for reading LE wire data |
| `writeLong` | writes BE bytes, no swap for BE mode | **✗ writes BE for MySQL** |
| `writeLength(l >= 16MB)` | uses `writeLong` → writes BE | **✗ big-endian for 8-byte length** |

`writeLong` is inconsistent with the other integer writers. For data > 16MB (length-encoded size), `writeLength` calls `writeLong` which emits big-endian bytes. In practice this only affects BLOB data > 16MB but violates the spec.

---

## 3. Text Protocol (ComQuery / DataRow)

`ComQuery` reads the query string, calls `MySQLExecutor.executeText(..., text=true)`.

**DataRow** (text protocol row):
```java
if (md.isByteData()) {
    resultBuffer.writeWithLength(Base64.getDecoder().decode(row.getBytes()));
} else {
    resultBuffer.writeWithLength(row.getBytes());
}
```
- Binary columns: base64-decoded → raw bytes → length-prefixed. ✓ (correct per MySQL text protocol)
- The `isByteData` flag on `ProxyMetadata` must be set correctly upstream; if it isn't, binary data is sent as-is base64 string

**ColumnDefinition for text protocol** (from `MySQLExecutor.executeQuery`):
```java
result.add(new ColumnDefinition(field, Language.UTF8_GENERAL_CI, false));
```
`binary=false` → column type is determined by `field.isByteData()`:
- `isByteData=true` → charset `Language.BINARY` (63), type from `toMysql()` ✓ (fixed)
- `isByteData=false` → charset `UTF8_GENERAL_CI`, type `MYSQL_TYPE_VAR_STRING`

---

## 4. Binary Protocol (ComStmtPrepare / ComStmtExecute / BinaryDataRow)

### ComStmtPrepare

`MySQLExecutor.prepareStatement` calls `connection.prepareStatement(query)`, reads parameter and result metadata via JDBC, then sends:
1. `ComStmtPrepareOk` (statement ID, num_params, num_columns)
2. `ColumnDefinition` for each parameter (`binary=true`)
3. `ColumnDefinition` for each result column (`binary=true`)

### ComStmtExecute — parameter reading

Wire format read order per iteration:
```
[loop per parameter]
  isNull from bitmap
  if not null:
    sendByteToServer (1 byte)          ← read per-parameter inside loop
    if sendByteToServer == 1:
      fieldType (1 byte)
      parameterFlag (1 byte)
    if isByteData: readBytesWithLength + Base64
    else: insertType(fieldType, ...)
```

**Protocol compliance issue**: MySQL spec says `new_params_bound_flag` is a **single** byte read **once** before all type info, not once per parameter. Reading it inside the loop is non-standard and may not interoperate with all clients. It works with MySQL Connector/J because Connector/J also reads it per-param (it sends type info only when flag changes), but it's not per-spec.

**Null bitmap on read** (client → server): `(paramFields.size() + 7) / 8` — correct (no +2 offset for client→server direction). ✓

### BinaryDataRow — result writing

**BUG — Null bitmap bit offset** (`BinaryDataRow.generateNullBitmap`):
```java
int nullBitmapSize = (rows.size() + 7 + 2) / 8;   // size accounts for +2 reserved bits ✓
...
BBufferUtils.setBit(newAr, i);                      // sets bit i, but should be i+2 ✗
```
Per MySQL binary protocol spec, the server→client null bitmap reserves bits 0 and 1. The size formula correctly adds 2, but the bit positions are written at `i` instead of `i+2`. Column 0's null bit lands in reserved bit 0 instead of bit 2. **Every null column will be reported at the wrong position.**

**BUG — Numeric types sent as strings** (`BinaryDataRow.buildMysqlBinaryType`):
```java
case FLOAT:
case DOUBLE:
case BIGINT:
case INTEGER:
case TINYINT:
case SMALLINT:
    resultBuffer.writeWithLength(row.getBytes());   // length-prefixed string ✗
    break;
```
MySQL binary protocol requires these types to be sent as **fixed-width binary** values:
- `MYSQL_TYPE_LONGLONG` (BIGINT) → 8 bytes LE
- `MYSQL_TYPE_LONG` (INTEGER) → 4 bytes LE
- `MYSQL_TYPE_SHORT` (SMALLINT) → 2 bytes LE
- `MYSQL_TYPE_TINY` (TINYINT) → 1 byte
- `MYSQL_TYPE_FLOAT` → 4 bytes IEEE 754 LE
- `MYSQL_TYPE_DOUBLE` → 8 bytes IEEE 754 LE

Sending them as length-prefixed ASCII strings (`"12345"`) causes the MySQL client to misparse the binary response.

DATE/TIME/TIMESTAMP are also in this group and likewise need their own binary encoding (length + year/month/day/etc. fields), not just `row.getBytes()`.

**BIT columns with n > 1** (`BinaryDataRow.buildMysqlBinaryType`):
```java
case BIT:
    resultBuffer.write((byte) (Boolean.parseBoolean(row) ? 0x01 : 0x00));
```
`isByteData=true` for `BIT(n>1)` means `row` is a base64 string (from `JdbcProxy`). `Boolean.parseBoolean(base64)` → always `false`. Multi-bit BIT columns always come back as 0.

---

## 5. ColumnDefinition Type Mapping

`toMysql()` maps JDBC type → MySQL wire type:

| JDBC Type | MySQL Type sent | Correct? |
|---|---|---|
| BOOLEAN, BIT | `MYSQL_TYPE_BIT` (0x10) | ✓ |
| BIGINT | `MYSQL_TYPE_LONGLONG` (0x08) | ✓ |
| INTEGER | `MYSQL_TYPE_LONG` (0x03) | ✓ |
| SMALLINT | `MYSQL_TYPE_SHORT` (0x02) | ✓ |
| TINYINT | `MYSQL_TYPE_TINY` (0x01) | ✓ |
| DOUBLE | `MYSQL_TYPE_DOUBLE` (0x05) | ✓ |
| FLOAT | `MYSQL_TYPE_FLOAT` (0x04) | ✓ |
| DATE | `MYSQL_TYPE_DATE` (0x0a) | ✓ |
| TIME, TIME_WITH_TIMEZONE | `MYSQL_TYPE_TIME` (0x0b) | ✓ |
| TIMESTAMP, TIMESTAMP_WITH_TIMEZONE | `MYSQL_TYPE_TIMESTAMP` (0x07) | ✓ |
| BLOB, LONGVARBINARY | `MYSQL_TYPE_BLOB` (0xfc) | ✓ |
| DECIMAL, NUMERIC | `MYSQL_TYPE_VAR_STRING` (0xfd) | **✗ should be MYSQL_TYPE_NEWDECIMAL (0xf6)** |
| CHAR | `MYSQL_TYPE_VAR_STRING` (0xfd) | **✗ should be MYSQL_TYPE_STRING (0xfe)** |
| All others (VARCHAR, CLOB, etc.) | `MYSQL_TYPE_VAR_STRING` (0xfd) | acceptable |

---

## 6. Auth / Handshake

`ConnectionEstablished` sends server greeting with:
- Protocol version 10
- Server version `"8.1.0"` (hardcoded)
- Auth plugin `"mysql_native_password"`
- Charset `UTF8_GENERAL_CI` (33)

`Auth` reads client handshake response:
- Stores client capabilities in context
- No real credential validation (always accepts)
- Sends back fake server capabilities covering all 4.1+ features

---

## 7. MySQLProtocol FSM

```
ProtoStateSequence:
  ConnectionEstablished
  ProtoStateSequence [optional]:
    SSLRequest (optional)
    SSLHandshake (optional)
  Auth
  ProtoStateWhile:
    NetworkWait (optional)
  ProtoStateWhile:
    Command
    ProtoStateSwitchCase:
      ComQuery, ComStmtPrepare, ComStmtExecute,
      ComRefresh, ComInitDb, ComPing, ComQuit
```

`MySQLProtocol.isBe()` returns `true` (constant `IS_BIG_ENDIAN = true`). MySQL is little-endian. `MySQLBBuffer` compensates via explicit LE methods but the flag name is misleading.

---

## 8. Bug Summary

| Bug | Location | Severity |
|---|---|---|
| Null bitmap bit offset wrong (`i` instead of `i+2`) | `BinaryDataRow.generateNullBitmap()` | **HIGH** — all null columns in prepared statement results misreported |
| Numeric types (INT/FLOAT/BIGINT/etc.) sent as strings in binary protocol | `BinaryDataRow.buildMysqlBinaryType()` | **HIGH** — prepared statement result values unreadable for numeric types |
| DATE/TIME/TIMESTAMP sent as string in binary protocol | `BinaryDataRow.buildMysqlBinaryType()` | **HIGH** — binary protocol temporal values malformed |
| `BIT(n>1)` always returns 0 | `BinaryDataRow.buildMysqlBinaryType()` | **HIGH** — `Boolean.parseBoolean(base64)` always false |
| `writeLong` writes big-endian | `MySQLBBuffer.writeLong()` | **MEDIUM** — only affects data > 16MB; violates LE spec |
| DECIMAL mapped to VAR_STRING not NEWDECIMAL | `ColumnDefinition.toMysql()` | **MEDIUM** — client type metadata wrong for decimal columns |
| CHAR mapped to VAR_STRING not STRING | `ColumnDefinition.toMysql()` | **LOW** — minor metadata mismatch |
| COM_STMT_CLOSE not handled | `MySQLProtocol.initializeProtocol()` | **MEDIUM** — prepared statement context never freed; memory leak |
| `new_params_bound_flag` read per-param not once | `ComStmtExecute.execute()` | **LOW** — works with Connector/J but non-spec |
| Server version hardcoded `"8.1.0"` | `ConnectionEstablished` | **LOW** — cosmetic |

---

## 9. Binary Data Full Path (MySQL-specific)

```
Client → Proxy:
  COM_QUERY or COM_STMT_EXECUTE
  └─ if BLOB param in binary protocol:
       ComStmtExecute reads readBytesWithLength() → Base64.encode → BindingParameter
  └─ JdbcProxy.convertObject decodes Base64 → byte[] → ps.setObject()

Proxy → Real MySQL (JDBC):
  rs.getBytes() → Base64.encode → SelectResult.records (List<List<String>>)

Proxy → Client:
  text protocol (COM_QUERY):
    DataRow: Base64.decode → writeWithLength(raw bytes)
    ColumnDefinition: charset=BINARY(63), type=MYSQL_TYPE_BLOB  [fixed]
  binary protocol (COM_STMT_EXECUTE):
    BinaryDataRow: Base64.decode → writeWithLength(raw bytes)   [correct]
    numeric types: writeWithLength(row.getBytes())              [BUG: strings not binary]
    null bitmap: bit i instead of bit i+2                       [BUG: wrong position]
```
