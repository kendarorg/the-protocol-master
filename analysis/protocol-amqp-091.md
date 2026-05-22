
## Package Overview

| Package | Purpose |
|---|---|
| `org.kendar.amqp.v09` | Entry points: `AmqpProtocol`, `AmqpProxy`, `AmqpProtocolSettings`, `AmqpJteResolver` |
| `context` | `AmqpProtoContext` — per-connection state (channels, consumeId, heartbeat) |
| `dtos` | `AmqpClasses`, `FrameType` — wire constants |
| `fsm` | `AmqpFrameTranslator` (bytes→`AmqpFrame`), `ProtocolHeader`, `events/AmqpFrame` |
| `messages/frames` | `Frame` (base), `MethodFrame`, `HeaderFrame`, `BodyFrame`, `HearthBeatFrame`, `GenericFrame` |
| `messages/methods/*` | One class per AMQP method: `connection/`, `channel/`, `basic/`, `queue/`, `exchange/` |
| `plugins` | `AmqpRecordPlugin`, `AmqpReplayPlugin`, `AmqpPublishPlugin`, `AmqpLatencyPlugin`, `AmqpNetErrorPlugin`, `AmqpReportPlugin`, `AmqpRestPluginsPlugin` |
| `plugins/apis` | `AmqpPublishPluginApis` — REST API to inject messages into live connections |
| `utils` | `AmqpProxySocket`, `FieldsReader/Writer`, `ShortStringHelper`, `LongStringHelper`, `ProxyedBehaviour`, `ConsumeConnected` |

---

## 1. Wire Protocol Flow

AMQP 0-9-1 is **big-endian**. Every frame has structure:

```
[type:1][channel:2][size:4][payload:size][frame-end:0xCE]
```

Frame types:
| Byte | Type |
|---|---|
| 1 | METHOD |
| 2 | HEADER |
| 3 | BODY |
| 8 | HEARTBEAT |

### Connection + Channel Handshake

```
Client → Proxy:   "AMQP\x00\x00\x09\x01"   (ProtocolHeader — BytesEvent)
Proxy  → Client:  Connection.Start
Client → Proxy:   Connection.StartOk
Proxy  → Client:  Connection.Tune
Client → Proxy:   Connection.TuneOk
Client → Proxy:   Connection.Open
Proxy  → Client:  Connection.OpenOk
─────────────── channel loop (Tagged "CHANNEL") ───────────────
Client → Proxy:   Channel.Open
Proxy  → Client:  Channel.OpenOk
─────────── per-channel switch/case loop ──────────────────────
  Queue.Declare / Queue.Bind / Queue.Unbind / Queue.Purge / Queue.Delete
  Exchange.Declare / Exchange.Bind / Exchange.Unbind / Exchange.Delete
  Basic.Consume   → spawns server-push callback path (see §3)
  Basic.Cancel
  Basic.Get
  Basic.Publish + HeaderFrame + BodyFrame   (3-frame publish sequence)
  Basic.Ack / Basic.Nack / Reject
  Heartbeat (interrupt state — matches at any point)
Client → Proxy:   Channel.Close → Channel.CloseOk
─────────────────────────────────────────────────────────────────
Client → Proxy:   Connection.Close → Connection.CloseOk
```

FSM defined in `AmqpProtocol.initializeProtocol()` using `ProtoStateSequence`, `ProtoStateWhile`, `ProtoStateSwitchCase`, and `Tagged`. `HearthBeatFrame` registered twice: as interrupt state (fires at any point) and inside the channel switch-case.

### Bytes → Frame translation

`AmqpFrameTranslator` is registered as an **interrupt state** on `BytesEvent`. It:
1. Rejects buffers starting with `AMQP` (those go to `ProtocolHeader` instead).
2. Peeks type/channel/size; throws `AskMoreDataException` if buffer too short.
3. Wraps the raw bytes in an `AmqpFrame` event and dispatches via `context.send()`.

All downstream states (`Frame` subclasses) consume `AmqpFrame`, not raw bytes.

---

## 2. Frame Class Hierarchy

```
Frame  (abstract, NetworkReturnMessage)
  ├── MethodFrame  (abstract — adds classId/methodId dispatch)
  │     ├── Connection.*  (classId=10)
  │     ├── Channel.*     (classId=20)
  │     ├── Exchange.*    (classId=40)
  │     ├── Queue.*       (classId=50)
  │     └── Basic.*       (classId=60)
  ├── HeaderFrame         (type=2)
  ├── BodyFrame           (type=3)
  └── HearthBeatFrame     (type=8)
```

`Frame.write(BBuffer)` serialises: writes type, channel, placeholder size, calls `writeFrameContent()`, back-patches size, writes `0xCE`.

`Frame.canRun(AmqpFrame)` checks type byte + delegates to `canRunFrame()` (MethodFrame checks classId+methodId; HeaderFrame/BodyFrame return true unconditionally).

Dual-mode flag `proxyed` (set via `asProxy()`) controls whether a frame instance is a client-side FSM handler or a server-side proxy handler (see §3).

---

## 3. Server-Push Callback Path (Subscriptions / BasicConsume)

This is the most complex part. AMQP consumers receive messages **asynchronously** — RabbitMQ pushes `BasicDeliver + HeaderFrame + BodyFrame` to the client at any time after `BasicConsume`. The proxy must intercept these and route them to the correct client connection.

### Step A — BasicConsume registration

`BasicConsume.executeMethod()`:
1. Parses queue/tag/flags from client frame.
2. Assigns a unique `consumeId` via `protocol.getCounter("CONSUME_ID")`.
3. Stores `basicConsume` in context under two keys:
   - `BASIC_CONSUME_CH_<channel>` — keyed by channel
   - `BASIC_CONSUME_CI_<consumeId>` — keyed by consumeId
4. Registers this context in `protocol.getContextsCache()` under `consumeId` (enables replay lookup).
5. Builds `consumeOrigin = queue + "|" + channel + "|" + serialized(arguments)` — stable identity across record/replay.
6. Calls `proxy.sendAndExpect(context, connection, basicConsume, basicConsumeOk)` — forwards to RabbitMQ, waits for `BasicConsumeOk`, copies back the assigned consumer tag.

### Step B — AmqpProxySocket handles server-initiated frames

`AmqpProxySocket` extends `NettyProxySocket`. Its `availableStates()` list registers proxy-mode handlers:

```java
new BasicDeliver()                  // NOT .asProxy() — has custom logic
new BasicCancel().asProxy()
new HeaderFrame().asProxy()
new BodyFrame().asProxy()
new ConnectionBlocked().asProxy()
new ConnectionUnblocked().asProxy()
new HearthBeatFrame().asProxy()
new BasicAck().asProxy()
new BasicNack().asProxy()
new BasicReturn().asProxy()
new BasicGetEmpty().asProxy()
```

`getStateToRetrieveOneSingleMessage()` returns `new GenericFrame()` which reads exactly one AMQP frame boundary from the TCP stream.

When RabbitMQ sends any of these, `AmqpProxySocket` matches the frame, builds an `AmqpFrame` event with `channel=-1` (sentinel meaning "from server"), and dispatches to the matching proxy state.

### Step C — BasicDeliver routing

`BasicDeliver.executeMethod()` (the **proxy-side** handler — `BasicDeliver` does not call `.asProxy()` because it needs custom logic):

```java
var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
var bd = new BasicDeliver();
// ... parse fields from RabbitMQ wire bytes
bd.setConsumeId(basicConsume.getConsumeId());
bd.setConsumeOrigin(basicConsume.getQueue() + "|" + basicConsume.getChannel() + "|" + ...) ;
proxy.respond(bd, new PluginContext("AMQP", "RESPONSE", ...));  // notify plugins (record)
return iteratorOfList(bd);  // write bd back to client
```

Key: `consumeId` links this delivery to the `BasicConsume` that registered it. `consumeOrigin` is the stable queue-identity string.

### Step D — HeaderFrame and BodyFrame (proxy mode)

Both check `isProxyed()`. When true:
1. Look up `BASIC_CONSUME_CH_<channel>` to get `consumeId` and `consumeOrigin`.
2. Set both on the frame.
3. Call `proxy.respond(frame, PluginContext)` — plugins can record.
4. Return `iteratorOfList(frame)` to forward to client.

`BodyFrame` also reads `CONTENT_TYPE_<channel>` (set by `HeaderFrame`) to decode content via `mapper.toGenericContent()`.

### Step E — ProxyedBehaviour helper

`ProxyedBehaviour.doStuff()` encapsulates the two-path logic used by simpler frames (Ack, Nack, etc.):

```java
if (input.isProxyed()) {
    // server-to-client path: attach consumeId, call proxy.respond(), return list
    var basicConsume = (ConsumeConnected) context.getValue("BASIC_CONSUME_CH_" + channel);
    ((ConsumeConnected) toSend).setConsumeId(basicConsume.getConsumeId());
    proxy.respond(toSend, pluginContext);
    return iteratorOfList(toSend);
}
// client-to-server path
return iteratorOfRunnable(() -> proxy.sendAndForget(context, connection, toSend));
```

`ConsumeConnected` is a marker interface (`getConsumeId()`/`setConsumeId()`) implemented by any frame that participates in the consumer callback chain: `BasicDeliver`, `HeaderFrame`, `BodyFrame`, `BasicCancel`, `BasicAck`, `BasicNack`.

---

## 4. Context State Keys

| Key pattern | Value | Set by |
|---|---|---|
| `CONNECTION` | `ProxyConnection` | connection handshake |
| `HEARTBEAT` | `short` | `ConnectionTuneOk` |
| `HEARTBEAT_LAST` | `long` | heartbeat timer |
| `BASIC_CONSUME_CH_<ch>` | `BasicConsume` | `BasicConsume.executeMethod` |
| `BASIC_CONSUME_CI_<id>` | `BasicConsume` | `BasicConsume.executeMethod` |
| `BASIC_CONSUME_CT_<origin>` | `String` consumerTag | `BasicConsume` / `BasicConsumeOk` |
| `CONTENT_TYPE_<ch>` | `String` | `HeaderFrame.executeFrame` |
| `BASIC_PUBLISH_RK_<ch>` | `String` routingKey | `BasicPublish.executeMethod` |
| `BASIC_PUBLISH_XC_<ch>` | `String` exchange | `BasicPublish.executeMethod` |
| `EXCHANGE_CH_<ch>` | `String` | queue/exchange bind operations |
| `ROUTING_KEYS_CH_<ch>` | `String` | queue bind |
| `QUEUE` | `HashSet<String>` | `BasicConsume.executeMethod` |

---

## 5. Heartbeat

`AmqpProtocol.start()` schedules a timer (every 5s) via `TimerService`. Each tick:
- Iterates all cached contexts.
- For contexts with `HEARTBEAT > 0`, compares `HEARTBEAT_LAST` with now + heartbeat interval.
- Writes a `HearthBeatFrame` to connected contexts.
- Removes disconnected contexts from the cache.

Heartbeat interval is negotiated during `ConnectionTuneOk`.

---

## 6. Plugins

| Plugin | Role |
|---|---|
| `AmqpRecordPlugin` | Extends `BasicRecordPlugin`. Skips infrastructure frames (`ConnectionStartOk`, `ChannelOpen`, `BasicPublish`, etc. in `toAvoid`). Tags storage items with `queue` (consumeOrigin), `consumeId`, `input`, `output` type names. |
| `AmqpReplayPlugin` | Extends `BasicReplayPlugin`. `hasCallbacks()=true` signals the framework that server-push callbacks exist. `sendBackResponses()` re-injects recorded `BasicDeliver+HeaderFrame+BodyFrame+BasicCancel` back to real client connections. Uses `realConnectionToRecorded` map (consumeId → context) for routing. `findConnection()` scans all contexts by consumeOrigin+exchange to locate the right one. |
| `AmqpPublishPlugin` | No-op `handle()`. Delegates entirely to `AmqpPublishPluginApis`. |
| `AmqpLatencyPlugin` | Injects artificial delay. |
| `AmqpNetErrorPlugin` | Injects network errors. |
| `AmqpReportPlugin` | Report generation. |
| `AmqpRestPluginsPlugin` | REST plugin management. |

### AmqpPublishPlugin REST API

`AmqpPublishPluginApis` exposes:

```
GET  /api/protocols/{instanceId}/plugins/publish-plugin/connections
     → lists all active contexts+channels with consumeId, exchange, consumerTag

POST /api/protocols/{instanceId}/plugins/publish-plugin/connections/{connectionId}/{channel}
     body: PublishAmqpMessage { contentType, appId, body, binary, exchange, queue, deliveryTag, ... }
     → synthesises BasicDeliver + HeaderFrame + BodyFrame and writes directly to the matching context
```

`doPublish()` logic:
1. Filters contexts by `connectionId` (0 = all).
2. For each channel with a `BASIC_CONSUME_CH_<ch>`, filters by `queue` prefix and `exchange`.
3. Deduplicates by consumerTag.
4. Builds `BasicDeliver + HeaderFrame + BodyFrame` and calls `context.write()` directly — bypasses the proxy, injects straight to client wire.

Binary bodies are Base64-decoded; text bodies taken as UTF-8 bytes.

---

## 7. Replay — Server-Push Reconstruction

During replay (`AmqpReplayPlugin.sendBackResponses()`):

1. Storage items are played back in order (optionally respecting `respectCallDuration` timing).
2. For `BasicDeliver`: looks up `realConnectionToRecorded` by `consumeId`. If absent, calls `findConnection()` which scans all contexts by `consumeOrigin` + `exchange`. Restores the live consumer tag from `BASIC_CONSUME_CT_<consumeOrigin>`.
3. For `HeaderFrame`, `BodyFrame`: same consumeId lookup.
4. For `BasicCancel`: consumeId lookup with fallback to `contextId`.
5. Calls `ctx.write(fr)` — pushes directly onto the client connection.

`repeatableItems` list: `ExchangeDeclare`, `QueueDeclare`, `QueueBind`, `ExchangeBind`, `BasicConsume`, `byte[]`, `ConnectionStartOk`, `ConnectionOpen`, `ChannelOpen` — these are re-executed on every replay pass rather than just once.

---

## 8. Publish Flow (client→broker)

`BasicPublish` only calls `proxy.sendAndForget()` — no response expected from broker. It stores routing key and exchange in context for `BodyFrame` to access:

```
context.setValue("BASIC_PUBLISH_RK_" + channel, routingKey);
context.setValue("BASIC_PUBLISH_XC_" + channel, exchange);
```

The 3-frame sequence `BasicPublish → HeaderFrame → BodyFrame` is a fixed `ProtoStateSequence` in the FSM. `BodyFrame.executeFrame()` reads `CONTENT_TYPE_<channel>` (set earlier by `HeaderFrame`) to decode the content.

---

## 9. Notable Design Choices / Gaps

| Item | Notes |
|---|---|
| `proxyed` flag | Single flag on `Frame` serves two completely different roles: "I am a proxy-side handler" vs "I am a client-side handler". Dual-use makes the logic in `executeFrame()` branchy. |
| `channel=-1` sentinel | `AmqpProxySocket.buildPossibleEvents()` passes `channel=-1` for server-originated frames. Proxy-side `executeMethod()` then reads the real channel from the wire buffer. |
| `BasicDeliver` not using `.asProxy()` | Unlike other callback frames, `BasicDeliver` has its own `executeMethod()` that fully handles the proxy path without calling `ProxyedBehaviour`. |
| `BodyFrame` dead branch | Lines 85-88 check `proxy.isReplayer() && isProxyed()` — unreachable because `isProxyed()=true` is already handled in the `if` above it. |
| No `BasicReturn` client-side handler | `BasicReturn` is registered only in `AmqpProxySocket` (server-push path). There is no client-side FSM handler — unroutable publishes from broker are silently forwarded but not inspectable via FSM. |
| Consumer tag negotiation | If broker assigns a different tag in `BasicConsumeOk`, `BasicConsume.executeMethod()` updates `BASIC_CONSUME_CT_<origin>` with the broker-assigned tag. Replay later restores this from context so re-injected `BasicDeliver` frames use the correct tag. |
| `getChannel()` auto-increment | `AmqpProtoContext.getChannel()` always increments `channel` field (starting at 1, first call returns 2). This is used when the proxy side needs to open new channels. |
