# protocol-redis Analysis

## Package Overview

| Package | Purpose |
|---|---|
| `org.kendar.redis` | `Resp3Protocol`, `Resp3Proxy`, `Resp3Context`, `RedisProtocolSettings`, `RedisJteResolver` |
| `fsm` | `Resp3MessageTranslator`, `Resp3PullState`, `Resp3Subscribe`, `Resp3Unsubscribe`, `Resp3Response`, `GenericFrame` |
| `fsm/events` | `Resp3Message` — carries both raw RESP3 string and parsed Java object |
| `parser` | `Resp3Parser`, `Resp3Input`, `Resp3ParseException`, `RespError`, `RespPush`, `RespVerbatimString` |
| `utils` | `Resp3ProxySocket` |
| `plugins` | `RedisRecordPlugin`, `RedisReplayPlugin`, `RedisPublishPlugin`, `RedisLatencyPlugin`, `RedisNetErrorPlugin`, `RedisReportPlugin`, `RedisRestPluginsPlugin` |
| `plugins/apis` | `RedisPublishPluginApis` — REST API to inject messages to subscribers |

---

## 1. Wire Format — RESP3

Redis uses a **text-based** protocol (RESP3). Each value starts with a type prefix byte, followed by data terminated by `\r\n`. No binary length headers — the protocol is self-delimiting by content.

| Prefix | Type | Example |
|---|---|---|
| `+` | Simple string | `+OK\r\n` |
| `-` | Simple error | `-ERR message\r\n` |
| `:` | Integer | `:42\r\n` |
| `$` | Bulk string | `$5\r\nhello\r\n` |
| `*` | Array | `*2\r\n:1\r\n:2\r\n` |
| `_` | Null | `_\r\n` |
| `#` | Boolean | `#t\r\n` / `#f\r\n` |
| `,` | Double | `,3.14\r\n` |
| `(` | Big number | `(1234567890\r\n` |
| `!` | Bulk error | `!ERR message\r\n` |
| `=` | Verbatim string | `=7\r\ntxt:foo\r\n` |
| `%` | Map | `%2\r\n+key\r\n:val\r\n...` |
| `~` | Set | `~2\r\n+a\r\n+b\r\n` |
| `>` | Push | `>3\r\n+message\r\n+chan\r\n+data\r\n` |

`isBe() = true` but endianness is irrelevant — the protocol is pure text/ASCII.

---

## 2. Parser — Resp3Parser + Resp3Input

`Resp3Input` is a cursor-based string wrapper. Key methods:
- `charAtAndIncrement()` — returns current char, advances index.
- `substring(n)` — returns n chars starting at index, advances by n.
- `length()` — remaining bytes.
- `getPreString()` — everything consumed so far (used to record raw RESP3 wire string).

`Resp3Parser.parse(Resp3Input)` dispatches on the first byte:

**Java type mapping:**

| RESP3 | Java type produced |
|---|---|
| Simple string | `String` |
| Simple error | `RespError` |
| Integer | `int` |
| Bulk string | `String` (or `null` for length=-1) |
| Array | `List<Object>` |
| Null | `null` |
| Boolean | `boolean` |
| Double | `double` |
| Big number | `BigInteger` |
| Bulk error | `RespError` |
| Verbatim string | `RespVerbatimString` {type, msg} |
| Map | `List<Object>` with sentinel `"@@MAP@@"` as first element; entries as `List.of(key, value)` |
| Set | `List<Object>` with sentinel `"@@SET@@"` as first element |
| Push | `RespPush extends ArrayList` with sentinel `"@@PUSH@@"` as first element |

Maps, sets and pushes are encoded as annotated `List` with a magic string sentinel to distinguish them from plain arrays during serialisation.

`Resp3ParseException.isMissingData()` — set `true` when the buffer is incomplete (unterminated). Callers throw `AskMoreDataException` on this flag.

---

## 3. Resp3Message — Dual Representation

`Resp3Message` carries two forms simultaneously:
- `data` — parsed Java object (e.g. `List`, `String`, `Integer`).
- `message` — the raw RESP3 wire string (result of `Resp3Input.getPreString()`).

`write(BBuffer)` writes `message` as raw ASCII — no re-serialization.

When constructed from `JsonNode` (used during replay), it round-trips through serializer+parser:
```
JsonNode → parser.serialize() → RESP3 string → parser.parse() → Java object
```
This ensures `data` matches what a real parse would produce.

---

## 4. Resp3MessageTranslator — Triple Role

`Resp3MessageTranslator` implements three interfaces:
- `InterruptProtoState` — fires on `BytesEvent` at any FSM point (client path).
- `NetworkReturnMessage` — can be written back.
- `NetworkProxySplitterState` — used by `GenericFrame` to extract exactly one message from the TCP stream (proxy path).

**canRun()**: fully parses the entire buffer as RESP3 to verify it's complete. Returns true or throws `AskMoreDataException`. Uses `rb.getAll()` (from position 0).

**execute()**: uses `rb.getRemaining()` (from current position), parses, advances buffer position by `input.getIndex()`, then:
- Client mode: `context.send(Resp3Message)` — async dispatch.
- Proxy mode (`.asProxy()`): returns `iteratorOfList(Resp3Message)` — synchronous.

**split()**: like `execute()` but returns a new `BytesEvent` wrapping exactly the parsed bytes, leaving the remainder in the original buffer. Used by `GenericFrame` to split a TCP stream into individual RESP3 messages.

**canRun() vs execute() position mismatch**: `canRun()` uses `rb.getAll()` (position-0), `execute()` uses `rb.getRemaining()` (current-position). If the buffer position was advanced between calls, execute reads from a different offset than canRun tested.

---

## 5. Protocol FSM

```
Interrupt: Resp3MessageTranslator(BytesEvent) → sends Resp3Message
─────────────── main while loop ────────────────────────────────────
  SwitchCase:
    Sequence:
      Resp3Subscribe(Resp3Message)   → canRun: first element == "subscribe"
      Resp3Unsubscribe(Resp3Message) → canRun: first element == "unsubscribe"
    Resp3PullState(Resp3Message)     → canRun: always true (not proxied)
─────────────────────────────────────────────────────────────────────
```

No connect/auth state — Redis clients send commands immediately. The FSM handles authentication commands as generic `Resp3PullState` requests forwarded to real Redis.

---

## 6. State Behaviour

### Resp3Subscribe

`canRun()`: checks `data instanceof List && list.get(0).equalsIgnoreCase("subscribe")`.

`execute()` (client path):
1. **Re-parses** `event.getMessage()` from scratch — redundant, the data is already parsed.
2. Extracts queue name from position 1, stores in context as `QUEUE` (single string, overwrites any previous).
3. Calls `proxy.sendAndExpect(context, connection, event, new Resp3Response())`.

`execute()` (proxy path, `isProxyed()`): returns `iteratorOfEmpty()` — discards. Broker's subscribe-ack is not forwarded.

### Resp3Unsubscribe

`canRun()`: checks `"unsubscribe"` at list position 0.

`execute()`: forwarded to real Redis. Does **not** clear `QUEUE` from context.

### Resp3PullState

`canRun()` (client): always `true`.
`canRun()` (proxy): checks `data instanceof List && list.get(0).equalsIgnoreCase("message")` — matches Redis pub/sub push messages from broker.

`execute()` (client): `proxy.sendAndExpect(context, connection, event, new Resp3Response())` — all non-subscribe commands.

`execute()` (proxy, message push): `proxy.respond(event.getData(), pluginContext)` then `iteratorOfList(event)` — records and forwards to client.

`execute()` (proxy, non-message): returns `iteratorOfEmpty()` — discards other server-initiated frames.

### Resp3Response

Used as the **"expected response"** template in `sendAndExpect`. It is a `ProtoState` registered to match any `Resp3Message` (canRun always true). When the response arrives from real Redis:
1. `execute()` stores the event in `this.event`.
2. `write()` writes `this.event.getMessage()` as ASCII — the raw RESP3 response bytes forwarded to client.

---

## 7. Server-Push Callback Path (Pub/Sub)

After `SUBSCRIBE`, Redis broker pushes `message` frames at any time.

### Proxy socket

`Resp3ProxySocket.availableStates()` = `[Resp3PullState().asProxy()]`.

`buildPossibleEvents()`: uses `translator.asProxy()` synchronously to parse raw bytes into a `Resp3Message`, returns it as the event.

`getStateToRetrieveOneSingleMessage()` = `new GenericFrame()` → uses `Resp3MessageTranslator.split()` to extract exactly one message per dispatch cycle.

### Push flow

1. Redis broker sends `*3\r\n+message\r\n+channel\r\n$data\r\n`.
2. `Resp3ProxySocket` parses → `Resp3Message` with `data = ["message", "channel", "data"]`.
3. `Resp3PullState.asProxy().canRun()` checks `list.get(0) == "message"` → true.
4. `execute()` calls `proxy.respond(data, pluginContext)` (record) then `iteratorOfList(event)`.
5. `event.write(buffer)` writes raw RESP3 push string to client.

---

## 8. Context State Keys

| Key | Value | Set by |
|---|---|---|
| `CONNECTION` | `ProxyConnection(Resp3ProxySocket)` | connection handshake |
| `QUEUE` | `String` (single channel name) | `Resp3Subscribe.execute()` |

Only one subscription tracked per context. Multi-channel subscriptions overwrite the key with the last channel name.

---

## 9. REST API Publish (RedisPublishPluginApis)

`doPublish(messageData, connectionId, queue)`:
1. Iterates all contexts. Filters by `connectionId` (-1 = all).
2. Builds `List.of("message", queue, dataToSend)` — simulates a Redis push message.
3. Serializes to RESP3 string via `parser.serialize(mapper.toJsonNode(data))`.
4. Calls `context.write(Resp3Message)` directly — bypasses proxy, writes to client wire.

**No topic filter**: if `connectionId == -1`, the message is sent to ALL connections regardless of their subscribed `QUEUE`. A subscriber to "chan-A" will receive a publish to "chan-B" if no `connectionId` filter is applied.

---

## 10. Plugins

### RedisRecordPlugin

`getData()` unwraps `Resp3Message` → returns `data` (Java parsed object). Jackson serializes this to JSON.

`buildTag()` inspects the parsed array:
- `["SUBSCRIBE", channel, ...]` → `{queue: channel, repeatable: true}`
- `["MESSAGE", channel, ...]` → `{queue: channel}`
- `["CLIENT", "SETINFO", ...]` → `{repeatable: true}`
- `["PING", ...]` → `{type: ping, repeatable: true}`

`shouldNotSave()`: skips `ping` items.

`asyncCall()` override: records server-push `RESPONSE` events asynchronously via `EventsQueue.send(WriteItemEvent)` — avoids blocking the callback delivery thread.

### RedisReplayPlugin

`hasCallbacks() = true`.

`buildState()`: calls `((Resp3Response) toread).execute(new Resp3Message(context, null, jsonNode))` — stores the replayed message in the `Resp3Response` object. The framework then calls `toread.write()` to send it to the client.

`sendBackResponses()`: starts with `Sleeper.sleep(10)` (10ms delay). Looks up context by `connectionId`, reconstructs `Resp3Message` from JSON, calls `ctx.write(fr)`.

`getContextTags()`: returns `{queue: QUEUE_value}` if context has a subscription.

`verifyContentRepeatable()`: returns `true` if `index.getTags().containsKey("repeatable")` — used to replay subscription confirmations and pings on every replay pass.

---

## 11. Serializer (Resp3Parser.serialize)

Converts `JsonNode` back to RESP3 wire format:

| JsonNode type | RESP3 output |
|---|---|
| Array with `@@MAP@@` sentinel | `%N\r\n` + key-value pairs |
| Array with `@@SET@@` sentinel | `~N\r\n` + elements |
| Array with `@@PUSH@@` sentinel | `>N\r\n` + elements |
| Array (other) | `*N\r\n` + elements |
| Integer/Long/Short | `:N\r\n` |
| Boolean | `#t\r\n` / `#f\r\n` |
| Float | `,F\r\n` (Float.parseFloat — loses double precision) |
| Double | `,D\r\n` |
| BigInteger | `(N\r\n` |
| Null | `_\r\n` |
| String | `$N\r\ndata\r\n` (always bulk, never simple `+`) |
| Object `{type, msg}` | verbatim or error format |

---

## 12. Bugs and Gaps

| Item | Detail |
|---|---|
| Double parse in `Resp3Subscribe` | `execute()` calls `parser.parse(event.getMessage())` to extract queue name — the data is already in `event.getData()`. Needless re-parse. |
| Single QUEUE per context | `context.setValue("QUEUE", list.get(1))` overwrites on every subscribe. Multi-channel subscriptions lose all but the last channel name. |
| `Resp3Unsubscribe` doesn't clear QUEUE | After UNSUBSCRIBE, `QUEUE` remains set. Context appears subscribed to publish plugin even after unsubscribing. |
| `doPublish()` no topic filter | Sends to all contexts matching `connectionId`, ignoring whether their `QUEUE` matches the target topic. Subscribers receive messages for topics they didn't subscribe to. |
| Float-before-double serializer branch | In `serialize()`, `valNode.isFloat()` branch appears before the more specific double check. `Float.parseFloat(valNode.asText())` truncates precision. Dead second double branch never reached for float values. |
| `canRun()` reads from pos 0, `execute()` from current pos | `canRun()` uses `rb.getAll()`, `execute()` uses `rb.getRemaining()`. If buffer position is non-zero at execution time, `execute()` parses a different offset than `canRun()` validated. |
| Simple strings always serialized as bulk | Commented-out `+` path. All strings become `$N\r\ndata\r\n`. Not protocol-incorrect but wastes bytes for short strings. |
| `Resp3Subscribe` sequence in FSM | Placed in a `ProtoStateSequence(Subscribe, Unsubscribe)` — means an unsubscribe must immediately follow every subscribe in the FSM. A subscribe without a subsequent unsubscribe in the same iteration falls through to PullState unexpectedly. |
| `Sleeper.sleep(10)` hardcoded in replay | `sendBackResponses()` always sleeps 10ms before replay, regardless of settings. No way to disable. |
