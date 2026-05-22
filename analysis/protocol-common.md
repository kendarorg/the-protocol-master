# protocol-common Analysis

## Package Overview

| Package | Purpose |
|---|---|
| `buffers` | Core binary buffer (BBuffer) with endianness support |
| `proxy` | Base proxy, plugin dispatch, socket abstractions |
| `protocol` | FSM engine: states, context, events, messages |
| `plugins` | Record, replay, mock, forward, rewrite, report base plugins |
| `storage` | File/null/encrypted storage, StorageItem, index |
| `settings` | Protocol/global settings POJOs |
| `events` | EventsQueue pub/sub |
| `annotations`, `apis`, `ui` | HTTP API layer, Swagger, HTMX UI |
| `utils` | JSON mapper, encryptor, timers, misc |

---

## 1. BBuffer (buffers)

Core binary buffer for all protocol byte I/O.

- Byte array with a position cursor; auto-expands on write
- Endianness (LE/BE) is per-instance, not global
- Provides typed reads/writes: int, short, long, float, double, ASCII, UTF-8
- KMP-based `indexOf` for byte-pattern search
- Hex-dump utility for debugging

**Concerns:**
- `getFloat()` reads 8 bytes but `byte2Float` only processes 4 → silent data loss
- Endianness is not serialized; replayed BBuffers must match original endianness implicitly
- No maximum size limit; unbounded growth under malformed input

---

## 2. Proxy Base (proxy)

Abstract base managing plugin dispatch and connection metadata.

**Plugin dispatch model:**
- Plugins registered per `ProtocolPhase` × `(inputType, outputType)` key
- Key built as `inputClass.getName() + "," + outputClass.getName()`; `Object.class` acts as wildcard
- `PluginHandler` invokes `handle(PluginContext, ProtocolPhase, in, out)` via reflection

**Concerns:**
- `getAllPlugins()` deduplicates by class, losing phase context
- No validation that proxy is initialized before plugin execution

---

## 3. Protocol FSM (protocol)

**ProtoContext** is the FSM engine:

```
BytesEvent (BBuffer)
  → findThePossibleNextState()
  → state.executeEvent(event)
  → Iterator<ProtoStep>
  → runSteps() → ReturnMessage
  → NetworkProtoContext.write()
```

- Execution stack is per-tag; supports parallel sub-protocol instances
- States marked `asOptional()` are skipped on failure rather than throwing
- Composite states: `ProtoStateWhile`, `ProtoStateSwitchCase`, `ProtoStateSequence`
- Recursion depth hard-capped at 10

**Concerns:**
- No built-in timeout; relies on socket-level 30s read timeout
- Max recursion of 10 may be too tight for deeply nested protocols

---

## 4. BasicRecordPlugin / BasicReplayPlugin (plugins)

**Record path:**
1. `PRE_CALL`: capture input → `mapper.toJsonNode(input)` → StorageItem
2. `POST_CALL`: capture output → `mapper.toJsonNode(output)` → StorageItem
3. Emit `WriteItemEvent` → FileStorageRepository queues disk write

**Replay path:**
1. `PRE_CALL`: load StorageItem from index by `(caller, type, tags)` match
2. `mapper.deserialize(storageItem.output, TargetClass)` → reconstruct object
3. Return reconstructed object; skip real proxy call

**Binary data serialization:**
- `BBuffer` instances → `BinaryNode` (Base64 in JSON) via special branch in `JsonMapper.toJsonNode()`
- Round-trip: `byte[]` → Base64 → JSON string → Base64 → `byte[]`

**Concerns:**
- Tag matching is fuzzy (confidence scoring, `equalsIgnoreCase`); can produce false matches
- `shouldNotSave()` naming is inverted relative to its usage at call site (logic smell)
- Async response replay uses sleeping threads; risk of thread pool exhaustion under load
- No checksum/hash to verify binary integrity after JSON round-trip
- Repeatable items (e.g., `CONNECTION`) have no deduplication; stored separately per occurrence

---

## 5. Storage (storage)

**On-disk layout:**
```
scenario/
├── 0000000001.<protocolInstanceId>.json   ← StorageItem (full exchange)
├── 0000000002.<protocolInstanceId>.json
└── index.<protocolInstanceId>.json        ← Array<CompactLine> (metadata only)
```

**StorageItem**: input (JsonNode) + output (JsonNode) + duration, type, caller, tags, timestamps

**CompactLine**: index metadata for fast lookup — index, type, caller, durationMs, tags

**Concerns:**
- No version field in StorageItem; schema changes silently break old recordings (Jackson `FAIL_ON_UNKNOWN_PROPERTIES=false`)
- CompactLine has no binary data size field; must deserialize full StorageItem to know payload size
- Tags are `Map<String,String>`; numeric values are stringified, losing type info
- FileStorageRepository uses a single-threaded executor; under write pressure, queue grows unbounded
- Timestamp precision is milliseconds; sub-ms timing is lost, affecting replay fidelity for fast protocols

---

## 6. Notable Cross-Cutting Issues

| Issue | Location | Impact |
|---|---|---|
| Endianness not serialized | `BBuffer` | Replayed buffers silently use wrong byte order if context differs |
| Float read reads 8 bytes, processes 4 | `BBuffer.getFloat()` | Data corruption for float columns |
| No resource-cleanup notification to plugins | `ProxySocket.close()` | Memory leaks if plugins cache large binary objects |
| No protocol version in storage | `StorageItem` | Silent deserialization failures after protocol changes |
| Fuzzy tag matching in replay | `BasicReplayPlugin` | Wrong recorded response may be returned |
| Binary data Base64 round-trip | `JsonMapper`, `BasicRecordPlugin` | Overhead; integrity unverified |
