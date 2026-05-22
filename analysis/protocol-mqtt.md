# protocol-mqtt Analysis

## Package Overview

| Package | Purpose |
|---|---|
| `org.kendar.mqtt` | Entry points: `MqttProtocol`, `MqttProxy`, `MqttProtocolSettings`, `MqttContext`, `MqttJteResolver` |
| `fsm` | All packet states: `Connect`, `ConnectAck`, `Publish`, `PublishAck/Rec/Rel/Comp`, `Subscribe`, `SubscribeAck`, `Disconnect`, `PingReq/Resp`, `GenericFrame`, `MqttPacketTranslator`, `BaseMqttState`, `BasePropertiesMqttState` |
| `fsm/events` | `MqttPacket` — the typed event carrying parsed fixed header + payload buffer |
| `fsm/dtos` | `Topic`, `Mqtt5Property` |
| `enums` | `MqttFixedHeader`, `ConnectFlag`, `Mqtt5PropertyType` |
| `plugins` | `MqttRecordPlugin`, `MqttReplayPlugin`, `MqttPublishPlugin`, `MqttLatencyPlugin`, `MqttNetErrorPlugin`, `MqttReportPlugin`, `MqttRestPluginsPlugin` |
| `plugins/apis` | `MqttPublishPluginApis` — REST API to inject PUBLISH to live subscribers |
| `utils` | `MqttBBuffer`, `MqttProxySocket`, `VarBValue` |

---

## 1. Wire Format

MQTT is **big-endian**. Every packet:

```
[fixed-header:1][remaining-length:1-4 VarBInt][variable-header...][payload...]
```

The fixed-header byte encodes packet type (upper 4 bits) + flags (lower 4 bits). For PUBLISH:

| Bit | Flag |
|---|---|
| 3 | DUP |
| 2-1 | QoS (0/1/2) |
| 0 | RETAIN |

`MqttFixedHeader.of(byte)` does **bitmask** matching (reverse scan, `(enum_value & input) == enum_value`), not exact equality. This handles PUBLISH variants (QoS bits set) resolving to `PUBLISH(0x30)`. `SUBSCRIBE(0x82)` and `UNSUBSCRIBE(0xA2)` have their reserved bits baked into the enum value.

### VarBInteger encoding

`MqttBBuffer.writeVarBInteger()` / `readVarBInteger()`: base-128 encoding, MSB of each byte signals continuation. Max value 268,435,455 (4 bytes). Used for remaining-length and MQTT5 properties block length.

### Strings

`readUtf8String()` / `writeUtf8String()`: 2-byte big-endian length prefix + UTF-8 bytes. Identical to MQTT spec.

---

## 2. Protocol Flow

```
Client → Proxy:  CONNECT
Proxy  → Client: CONNACK   (via proxy.sendAndExpect → RealBroker)
────────── main while loop ──────────────────────────────────────
  PingReq    → PingResp  (proxy.sendAndForget)
  Subscribe  → SubscribeAck
  Publish (QoS 0) → sendAndForget
  Publish (QoS 1) → PublishAck  (via sendAndExpect)
  Publish (QoS 2) → PublishRec → [PublishRel optional] → PublishComp
  PublishAck.asProxy()    ← broker ack for server-push QoS1
  PublishRec+Comp.asProxy() ← broker QoS2 handshake for server-push
────────────────────────────────────────────────────────────────
  Disconnect (interrupt state — any point)
```

FSM defined in `MqttProtocol.initializeProtocol()`. Nested `Tagged(Tag.ofKeys("PACKET"), ...)` scopes packet-identifier tracking. `Disconnect` is registered as interrupt state (fires at any point, no explicit state machine step needed).

There is a commented-out `addInterruptState(new Disconnect(PingReq.class))` — incomplete attempt to handle disconnect during ping.

---

## 3. Bytes → Packet Translation

`MqttPacketTranslator` is registered as an **interrupt state** on `BytesEvent`. It operates in two modes controlled by `.asProxy()`:

| Mode | Behaviour |
|---|---|
| **client** (default) | Calls `context.send(new MqttPacket(...))` — async event dispatch; returns `iteratorOfEmpty()` |
| **proxy** (`.asProxy()`) | Returns `iteratorOfList(new MqttPacket(...))` — synchronous, caller gets the packet directly |

`canRun()`: peeks 1-byte flag + VarBInt length, throws `AskMoreDataException` if buffer incomplete.

`execute()` also extracts the **packet identifier** (based on type) and calls `context.usePacket(id)` to track used IDs.

---

## 4. Frame Class Hierarchy

```
BaseMqttState  (abstract, NetworkReturnMessage)
  ├── BasePropertiesMqttState  (adds MQTT5 properties read/write)
  │     ├── Connect
  │     ├── ConnectAck
  │     ├── Publish
  │     ├── PublishAck / PublishRec / PublishRel / PublishComp
  │     ├── Subscribe / SubscribeAck
  │     ├── Disconnect
  │     └── PingReq / PingResp
  └── (GenericFrame — splitter, not a state proper)
```

`BaseMqttState.write(BBuffer)` serialises: writes fixed-header byte, serialises content to a temp `MqttBBuffer`, writes VarBInt length, writes content. Back-patching not needed — uses temp buffer.

`canRun(MqttPacket)`: compares `event.getFixedHeader().getValue() == getFixedHeader().getValue()`.

`asProxy()` flag on `BaseMqttState` (mirrors AMQP pattern) — controls server-push vs client path in `executeFrame()`.

---

## 5. Server-Push Callback Path (Subscriptions)

Unlike AMQP (many callback frame types), MQTT broker only pushes **PUBLISH** to subscribers. The entire subscription callback is handled by one state.

### Step A — Subscribe registration

`Subscribe.executeFrame()`:
1. Reads packet identifier + MQTT5 properties.
2. Reads topic list (UTF-8 string + options byte per topic).
3. Stores each topic as `options + "|" + topic` in context key `"TOPICS"` (HashSet).
4. Calls `proxy.sendAndExpect(context, connection, subscribe, subscribeAck)` — forwards to broker, waits for SUBACK.

### Step B — MqttProxySocket

`MqttProxySocket` registers only **one** proxy-side state:

```java
new Publish().asProxy()
```

The translator in proxy mode (`new MqttPacketTranslator().asProxy()`) is used directly in `buildPossibleEvents()` — it translates raw bytes synchronously to `MqttPacket`, then `Publish.asProxy()` matches and handles it.

All other broker-initiated packets (PUBACK for server-push QoS1, PUBREC/PUBCOMP for QoS2) are handled via `expectPubAck` / `expectPubRec` on the `MqttProxy` side (see §7).

### Step C — Publish (proxy mode)

`Publish.executeFrame()` when `isProxyed()`:
```java
proxy.respond(publish, new PluginContext("MQTT", "RESPONSE", ...));  // notify plugins
return iteratorOfList(publish);  // write to client
```

No topic routing needed — the broker already sent it to the correct subscriber connection. The proxy just records and forwards.

### Step D — QoS handling in Publish (client→broker)

| QoS | Proxy call |
|---|---|
| 0 | `proxy.sendAndForget()` — no ack expected |
| 1 | `proxy.sendAndExpect(publish, PublishAck)` |
| 2 | `proxy.sendAndExpect(publish, PublishRec)` → `PublishRel.asOptional()` in FSM |

For QoS2, `PublishRel` is registered as **optional** in the FSM sequence after `Publish` — it only runs if a PUBREC comes back before the next packet.

---

## 6. MQTT5 Properties

`BasePropertiesMqttState.readProperties()` / `writeProperties()` only activates when `protocolVersion == 5`. Properties are a VarBInt-prefixed block of typed TLV entries. `Mqtt5PropertyType` enum + `Mqtt5Property` handle serialisation. Protocol version stored on both `MqttContext` and each frame instance (set during `Connect.executeFrame()`).

---

## 7. REST API Publish (MqttPublishPluginApis)

`doPublish(messageData, connectionId, topic)`:
1. Iterates all cached contexts.
2. For each, checks `TOPICS` set — looks for entry ending with `"|" + topic`.
3. Filters by `connectionId` (-1 = all).
4. Gets packet ID from `context.packetToUse()`.
5. Builds `Publish` with correct QoS flags, calls `.asProxy()` on the message object.
6. **QoS1**: registers `expectPubAck(context, pubRel)` — proxy will wait for client's PUBACK.
7. **QoS2**: registers `expectPubRec(context, pubRel)` — proxy handles the 4-way handshake.
8. Calls `context.write(message)` directly — bypasses normal proxy flow, writes to client wire.

### Packet ID management — MqttContext.packetToUse()

Unusual algorithm:
1. Sorts `usedPackets` ascending.
2. Scans from tail backwards while IDs > 14000.
3. Returns `maxFound - 1` (one below the highest recycled ID).
4. If no IDs > 14000 found, clears the set and returns 27999.

This recycles IDs from the **high range** (14001–28000), keeping low IDs free for the client's own packets. Avoids collisions between client-originated and proxy-injected packet IDs.

---

## 8. Context State Keys

| Key | Value | Set by |
|---|---|---|
| `CONNECTION` | `ProxyConnection` | connection handshake |
| `PROTOCOL_VERSION` | `int` | `Connect.executeFrame` via `MqttContext.setProtocolVersion()` |
| `TOPICS` | `HashSet<String>` (`"options|topic"`) | `Subscribe.executeFrame` |

---

## 9. Plugins

| Plugin | Notes |
|---|---|
| `MqttRecordPlugin` | Skips `Disconnect` and `PingReq`. Tags storage by topic/QoS from `topicName` or `topics` fields. `consumeId` lookup from output JSON (rarely set). |
| `MqttReplayPlugin` | `hasCallbacks()=true`. `sendBackResponses()` replays `ConnectAck` and `Publish` frames. Lookup by `connectionId` from `contextsCache`. Topic tags for context matching. `repeatableItems`: `Connect`, `Subscribe`, `Publish`. |
| `MqttPublishPlugin` | REST-only, delegates entirely to `MqttPublishPluginApis`. |

---

## 10. Bugs and Gaps

| Item | Detail |
|---|---|
| `getContextTags()` regex bug | `topic.split("|", 2)` — `|` is regex "or"; matches empty string at every position. Limit 2 yields `["", "1|mytopic"]`. Then `spl[0]` is `""` and `spl[1].substring(1)` is `"1|mytopic".substring(1)` = `"|mytopic"`. QoS always empty, topic has leading `|`. Should be `split("\\|", 2)`. Only affects `MqttReplayPlugin.getContextTags()` (replay context matching). |
| `cleanSession` not implemented | `Connect.executeFrame()` detects `cleanSession` flag but has `//TODOMQTT clean all sessions` comment — no action taken. |
| `Unsubscribe` not in FSM | `MqttFixedHeader` has `UNSUBSCRIBE`/`UNSUBACK` and translator handles the packet ID, but no FSM state handles `Unsubscribe`. Topics are never removed from `TOPICS`. |
| `packetIdentifier` tags commented out | Both `MqttRecordPlugin.buildTag()` and `MqttReplayPlugin.buildTag()` have `//data.put("packetIdentifier", ...)` commented out — packet IDs are not stored as tags. |
| Only MQTT3/5 | `MqttProtocol.VERSION_3 = 3`, `VERSION_5 = 5`. MQTT 3.1 (version=3) and 3.1.1 (version=4) differ; 4 is never referenced. Any client sending protocol level 4 will be treated as 3 (no properties, no issues, but version check `isVersion(5)` returns false correctly). |
| AUTH packet | `MqttFixedHeader.AUTH(0xF0)` defined but no handler. MQTT5 enhanced auth not supported. |
