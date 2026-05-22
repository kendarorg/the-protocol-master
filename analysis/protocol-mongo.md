# protocol-mongo Analysis

## Package Overview

| Package | Purpose |
|---|---|
| `org.kendar.mongo` | Entry points: `MongoProtocol`, `MongoProxy`, `MongoDirectProxy`, `MongoProtocolSettings` |
| `fsm` | `MongoState` (base), `OpMsg`, `OpQuery`, `OpCompressed`, `StandardOpMsgCommand`, `MongoProtoContext` |
| `fsm/events` | `OpMsgRequest`, `OpQueryRequest`, `CompressedDataEvent` |
| `fsm/msg` | `CmdHello`, `CmdSaslStart`, `CmdSaslContinue`, `CmdGeneric` — OP_MSG command handlers |
| `fsm/query` | `QueryHello` — OP_QUERY handler |
| `dtos` | `BaseMessageData`, `OpMsgContent`, `OpMsgSection`, `OpQueryContent`, `OpReplyContent`, `MongoCommandsConstants`, `MongoCommandType` |
| `messages` | `OpReply` — stub, write() is empty |
| `executor` | `MongoExecutor` — bridge between FSM and `MongoProxy` |
| `compressors` | `CompressionHandler`, `NoopCompressionHandler`, `SnappyCompressionHandler`, `ZlibCompressionHandler`, `ZStdCompressionHandler` |
| `proxy` | `DocumentContainer` — helper DTO |
| `plugins` | `MongoRecordPlugin`, `MongoReplayPlugin`, `MongoLatencyPlugin`, `MongoNetErrorPlugin`, `MongoReportPlugin`, `MongoRestPluginsPlugin` |

---

## 1. Key Differences vs Other Protocols

**`MongoProxy extends Proxy`** (not `NetworkProxy`). There is no TCP-level socket proxy. Instead, `MongoProxy` holds a MongoDB Java driver `MongoClient` and executes commands via the driver API. This means:

- No `AmqpProxySocket` / `MqttProxySocket` equivalent.
- No server-push callbacks — MongoDB is purely request/response.
- Authentication is handled locally by the proxy (faked), not forwarded.
- Real connection is created lazily on first command (`isLateConnect = true`).

**Little-endian** — `IS_BIG_ENDIAN = false`. All multi-byte integers in MongoDB wire protocol are LE.

**BSON ↔ JSON bridge**: All BSON payloads are immediately decoded to Extended JSON strings via the MongoDB Java driver codec. All internal data structures hold JSON strings. When writing back, JSON is re-parsed to BSON.

---

## 2. Wire Format

Every MongoDB message:

```
[messageLength:4 LE][requestId:4 LE][responseTo:4 LE][opCode:4 LE][payload...]
```

Total header: 16 bytes. `MongoState.canRun()` peeks `length` at offset 0 and `opCode` at offset 12.

| OpCode | Value | Description |
|---|---|---|
| OP_REPLY | 1 | Server response (legacy) |
| OP_UPDATE | 2001 | (legacy, not handled) |
| OP_INSERT | 2002 | (legacy, not handled) |
| OP_QUERY | 2004 | Legacy query / initial handshake |
| OP_DELETE | 2006 | (legacy, not handled) |
| OP_COMPRESSED | 2012 | Compressed wrapper |
| OP_MSG | 2013 | Modern command transport |

---

## 3. Protocol Flow

```
Client → Proxy:  [optional OP_COMPRESSED wrapper, decompressed inline]
                 ↓
                 OP_MSG or OP_QUERY (switchcase, both optional)

OP_MSG path:
  OpMsg parses → OpMsgRequest event
  SwitchCase on OpMsgRequest:
    CmdHello      (has "hello" + "helloOk" fields) → runHelloOpMsg
    CmdSaslStart  (has "saslStart") → local auth handling
    CmdSaslContinue (loop, optional) → local auth continuation
    CmdGeneric    (everything else) → runGenericOpMsg → real MongoDB

OP_QUERY path:
  OpQuery parses → OpQueryRequest event
  QueryHello (has "helloOk": true) → runHelloOpQuery
```

No channel, session, or subscription loop. Each request-response is independent. The FSM is a flat `ProtoStateWhile` with `ProtoStateSwitchCase` — no long-lived state.

---

## 4. Event Chain

Three event types form a two-tier dispatch:

```
BytesEvent / CompressedDataEvent
  → MongoState.canRun() — checks opCode at offset 12
  → OpMsg.executeStandardMessage()  → context.send(OpMsgRequest)
  → OpQuery.executeStandardMessage() → context.send(OpQueryRequest)
  → OpCompressed.executeStandardMessage() → context.send(CompressedDataEvent)
                                             ↑ re-enters MongoState chain

OpMsgRequest / OpQueryRequest
  → StandardOpMsgCommand.canRun() — inspects JSON fields in section documents
  → CmdHello / CmdSaslStart / CmdSaslContinue / CmdGeneric / QueryHello
```

`MongoState` accepts both `BytesEvent` and `CompressedDataEvent` — the compressed event just delegates to the bytes path via adapter constructors.

---

## 5. OP_COMPRESSED

`OpCompressed.executeStandardMessage()`:
1. Reads original opCode, uncompressed size, compressorId.
2. Decompresses with matching handler (Snappy/Zlib/ZStd/Noop).
3. Rebuilds standard 16-byte header with original opCode + decompressed payload.
4. Emits `CompressedDataEvent` — the subsequent `OpMsg`/`OpQuery` states handle it transparently.

Compression handlers are keyed by `CompressorIds` integer constants. `OpCompressed.asOptional()` in the FSM means it only fires when the opCode is `OP_COMPRESSED`, otherwise the loop proceeds to the OP_MSG/OP_QUERY switch.

---

## 6. OP_MSG Parsing (OpMsg)

`OpMsg.executeStandardMessage()`:
1. Reads header (length, requestId, responseTo, opCode, flagBits).
2. Loops over payload reading sections:
   - **Type 0** (payloadType=0): reads 4-byte BSON doc length, decodes BSON → Extended JSON string, adds to section.documents.
   - **Type 1** (payloadType=1): reads 4-byte sequenceSize, reads identifier (null-terminated UTF-8 string via `BBuffer.getUtf8String()`), reads remaining bytes as multiple BSON documents.
3. Wraps in `OpMsgContent`, dispatches `OpMsgRequest`.

---

## 7. Proxy — MongoProxy

`doConnect(context)`: checks `CONNECTION` value in context. If absent, fires `CONNECT` phase plugins (replay can intercept). If no plugin handles it, calls `connect()` which builds a `MongoClient` from the connection string.

**`runGenericOpMsg()`**:
1. Extracts `$db` from section-0 document.
2. Merges section-1+ documents into the command as BSON arrays (keyed by section identifier).
3. Strips `$db`, `lsid`, `$clusterTime`, `apiVersion` from the command.
4. Fires `PRE_CALL` plugins — replay can short-circuit here.
5. Runs `database.runCommand(command)` via Java driver.
6. Fires `POST_CALL` plugins.
7. Returns `OpMsgContent` with one section-0 document containing the result JSON.

Special case: if command is `isMaster`, returns `null` (client discards it).

**`runHelloOpMsg()`** synthesizes a fake ismaster/hello response:
- Calls `hostInfo` on the real DB to get server time.
- Hardcodes: `maxWireVersion=8`, `maxBsonObjectSize` from server description, `connectionId` from `MONGO_PID` context value.
- `ismaster=true`, `readOnly=false`.
- `saslSupportedMechs` commented out (would expose auth mechanisms).

**`runHelloOpQuery()`**: same synthesis, but returns `OpReplyContent` (OP_REPLY format) instead of `OpMsgContent`.

---

## 8. Authentication (Local / Faked)

Auth is handled entirely by the proxy — credentials are **not** forwarded to real MongoDB. The MongoDB Java driver handles its own auth when connecting.

| Mechanism | Handling |
|---|---|
| PLAIN | Parses null-terminated `\0authid\0user\0password` from binary payload. Stores in context. Returns fake success immediately. |
| MONGODB-X509 | Returns fake success (no certificate verification). |
| GSSAPI | Returns fake success (no Kerberos). |
| SCRAM-SHA-1/256 | `handleScramAuthentication()` returns `null` → **NPE / broken**. |

`generateSuccessMessage()` response: `{ conversationId, done: true, payload: "r=<nonce>,s=QUI=,i=4096", ok: true }`. The `s=QUI=` is a hardcoded fake salt (base64 "AB").

`CmdSaslContinue` is registered in the FSM as a `ProtoStateWhile` loop with `.asOptional()`, meaning it fires zero or more times after `CmdSaslStart`.

---

## 9. Serialisation (Write Path)

`BaseMessageData.write(BBuffer)`:
```
[9999:4 LE]  ← placeholder, never back-patched
[requestId:4 LE]
[responseId:4 LE]
[opCode:4 LE]
[flags:4 LE]
```

`OpMsgContent.write(BBuffer)` — after super:
- Type-0 section: `[0x00][bsonBytes]`
- Type-1 section: `[0x01][identifierBytes][bsonDocs...]` — then back-patches size at `blockPos`; but `blockPos` is the position of the identifier start, not a reserved size field. This **overwrites the first 4 bytes of the identifier** with the block length.

`OpReplyContent.write(BBuffer)`:
```
[super: 9999 + ids + opCode + flags]
[cursorId:8 LE]
[startingFrom:4 = 0]
[numDocuments:4]
[bsonDocs...]
[writeInt(position, 0)]   ← writes 0 at CURRENT end position, not at offset 0 — length never fixed
```

`OpReply.write(BBuffer)` — **empty, writes nothing**.

---

## 10. Context State Keys

| Key | Value | Set by |
|---|---|---|
| `CONNECTION` | `ProxyConnection(MongoClient)` | `MongoProxy.doConnect()` |
| `MONGO_PID` | `ProcessId` | `MongoProtoContext` constructor |
| `CONVERSATION_ID` | `int` | `CmdSaslStart.generateSuccessMessage()` |
| `userid` | `String` | `CmdSaslStart` (PLAIN only) |
| `password` | `String` | `CmdSaslStart` (PLAIN only) |

`MongoProtoContext.getReqResId()` / `getNewPid()` use `descriptor.getCounter(...)` — global atomic counters, not per-context.

---

## 11. Plugins

| Plugin | Notes |
|---|---|
| `MongoRecordPlugin` | `getData()` calls `BaseMessageData.serialize()` — serializes to JSON including `opCode`, `flags`, sections. `shouldNotSave()` not overridden — records everything. Uses `BasicRecordPluginSettings` (synchronous, not async). |
| `MongoReplayPlugin` | `buildState()` handles `OP_MSG`, `HELLO_OP_MSG`, `HELLO_OP_QUERY`. Deserializes from stored JSON via `doDeserialize()`. No `hasCallbacks()` override — synchronous replay only. |

`MongoCommandsConstants` enum maps 100+ MongoDB command names to `MongoCommandType` categories (aggregation, QueryAndWrite, admin, etc.), but **nothing in the codebase reads this enum** — it is unused dead code.

---

## 12. Bugs and Gaps

| Item | Detail |
|---|---|
| `BaseMessageData.write()` length placeholder | Writes `9999` at offset 0-3; never back-patched. All sent messages have wrong length header. Likely works because MongoDB Java driver receiving the response ignores the length field (uses BSON internal length). |
| `OpMsgContent.write()` type-1 size field | `blockPos` points to start of identifier bytes; `writeInt(blockPos, blockLen)` overwrites the first 4 bytes of the identifier with the size. Wire format should be: 4-byte size FIRST, then identifier + null terminator, then documents. |
| `OpReplyContent.write()` length | `writeInt(position, 0)` writes 0 at end-of-buffer position, not at offset 0. Length never corrected. |
| `OpReply.write()` | Empty — never writes any bytes. |
| `CmdSaslContinue.canRun()` | Checks `saslStart != null` — same condition as `CmdSaslStart`. Should check `saslContinue`. This means `CmdSaslContinue` fires on the initial saslStart request (after `CmdSaslStart` already handled it), not on the continuation. |
| `CmdSaslStart.handleScramAuthentication()` | Returns `null`. SCRAM-SHA-1 and SCRAM-SHA-256 auth → NPE in caller. |
| `MongoReplayPlugin.buildState()` HELLO_OP_QUERY | Casts `in` as `OpMsgContent` but in the OP_QUERY path `in` is `OpQueryContent`. ClassCastException at replay time. |
| `QueryHello.canRun()` | Requires `helloOk == true`. Other OP_QUERY commands (e.g. legacy `find`) have no FSM handler — silently swallowed. |
| `MongoCommandsConstants` enum | 100+ command → category mappings, but **never referenced** anywhere in the codebase. |
| Legacy ops not handled | `OP_UPDATE(2001)`, `OP_INSERT(2002)`, `OP_DELETE(2006)` — in the enum but no FSM state. Modern MongoDB drivers use `OP_MSG` exclusively, so this is low risk. |
