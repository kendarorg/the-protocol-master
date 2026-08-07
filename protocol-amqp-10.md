# Implementation Plan: `protocol-amqp-10` — AMQP 1.0 protocol module

## 1. Context and goal

The Protocol Master is a multi-protocol proxy simulator (record/replay/mock of real infrastructure) built on an FSM framework in `protocol-common`. It currently supports AMQP 0.9.1 through the `protocol-amqp-091` module.

This plan adds a sibling module **`protocol-amqp-10`** implementing **AMQP 1.0** (OASIS standard, ISO/IEC 19464). AMQP 1.0 is *not* an incremental revision of 0.9.1 — it is a completely different wire protocol:

| Aspect | AMQP 0.9.1 | AMQP 1.0 |
|---|---|---|
| Spec owner | AMQP working group | OASIS / ISO 19464 |
| Handshake | `AMQP\x00\x00\x09\x01` + Connection.Start/Tune/Open | Protocol headers (`AMQP%d3 1.0.0` SASL layer, then `AMQP%d0 1.0.0`) |
| Authentication | Inside Connection.StartOk | Separate SASL layer with its own frames |
| Frame | type(1) channel(2) size(4) payload, end marker `0xCE` | size(4, includes itself) DOFF(1) type(1) channel(2) body, **no end marker** |
| Semantics | Classes/methods (Basic.Publish, Queue.Declare…) | Performatives (open, begin, attach, flow, transfer, disposition, detach, end, close) |
| Encoding | Fixed field tables (shortstr, longstr, field-table) | Self-describing recursive type system (format codes, described types) |
| Model | Channels → queues/exchanges | Connection → sessions (channels) → links (handles), credit-based flow control |

Decisions confirmed:
- **Test broker: ActiveMQ Artemis** (native AMQP 1.0 broker) via testcontainers.
- **Scope: full parity with `protocol-amqp-091`** — passthrough proxy, complete type codec, record/replay, all 7 plugins, plugin CLIs, JTE UI panels, CLI handler, docs and runner integration.

## 2. Naming conventions

| Item | Value |
|---|---|
| Module directory / artifactId | `protocol-amqp-10` |
| Root package | `org.kendar.amqp.v10` |
| Class prefix | `Amqp10*` (distinct simple names vs v09 helps logs and DI) |
| Protocol id / DI tag | `"amqp10"` |
| Default port | 5672 (5671 TLS) |
| Test proxy fake port | 5692 |
| JTE templates | `src/main/resources/jte/amqp10/` |

## 3. Framework integration model (verified against the codebase)

Discovery is via the custom DI container, not SPI:

- `@TpmService(tags = "amqp10")` on protocol, settings, CLI handler and every plugin.
- `@TpmConstructor` on the protocol constructor, with `@TpmNamed(tags = "amqp10") List<BasePluginDescriptor> plugins` injecting the plugin set (see `protocol-amqp-091/src/main/java/org/kendar/amqp/v09/AmqpProtocol.java`).
- `protocol-runner`'s `Main` resolves `ProtocolSettings` and `NetworkProtoDescriptor` instances by tag = the `"protocol"` string from the JSON settings file.

Base classes to extend (all in `protocol-common`):

| New class | Extends / implements |
|---|---|
| `Amqp10Protocol` | `NetworkProtoDescriptor` |
| `Amqp10ProtocolSettings` | `ByteProtocolSettingsWithLogin` |
| `Amqp10Proxy` | `NetworkProxy` |
| `context/Amqp10ProtoContext` | `NetworkProtoContext` |
| `cli/Amqp10CommandLineHandler` | `NetworkProtocolCommandLineHandler` |
| `Amqp10JteResolver` | `JteResolver` |
| `fsm/Amqp10FrameTranslator` | `ProtoState` + `InterruptProtoState` |
| `fsm/events/Amqp10Frame` | `ProtocolEvent` (carries channel **and** frame type AMQP/SASL) |
| `messages/Amqp10BaseFrame` | `ProtoState` + `NetworkReturnMessage` (analog of v09 `Frame`) |
| `messages/GenericFrame` | `NetworkProxySplitterState` |
| `utils/Amqp10ProxySocket` | `NettyProxySocket` |
| `plugins/Amqp10RecordPlugin` | `BasicRecordPlugin<BasicAysncRecordPluginSettings>` |
| `plugins/Amqp10ReplayPlugin` | `BasicReplayPlugin<BasicAysncReplayPluginSettings>` |
| other plugins | `ProtocolPluginDescriptorBase<...>` (as in v09) |

### Copy nearly verbatim from v09 (rename + retag only)
`pom.xml`, `Amqp10ProtocolSettings`, `Amqp10Proxy`, `Amqp10JteResolver`, `Amqp10CommandLineHandler`, `Amqp10ProtoContext` skeleton, plugins Report / Latency / NetError / RestPlugins, all 4 `plugins/cli/*` classes, JTE templates, test scaffolding, README structure.

### Redesign completely (wire protocol)
Frame translator, protocol-header + SASL handshake, type codec, all frame/performative classes, proxy-socket state list, record/replay tag correlation.

## 4. New module layout

```
protocol-amqp-10/
├── pom.xml                  parent org.kendar.protocol:protocol-master; deps: pf4j,
│                            protocol-common, protocol-test (test), qpid-jms-client (test)
├── README.md                module doc (pattern: protocol-amqp-091/README.md)
└── src/
    ├── main/java/org/kendar/amqp/v10/
    │   ├── Amqp10Protocol.java
    │   ├── Amqp10ProtocolSettings.java
    │   ├── Amqp10Proxy.java
    │   ├── Amqp10JteResolver.java
    │   ├── cli/Amqp10CommandLineHandler.java
    │   ├── context/Amqp10ProtoContext.java        (+ channel→session, handle→link maps)
    │   ├── dtos/FrameType.java                    (AMQP=0, SASL=1)
    │   ├── dtos/Performatives.java                (descriptor codes, see §5)
    │   ├── fsm/ProtocolHeader.java                (SASL + AMQP header negotiation)
    │   ├── fsm/Amqp10FrameTranslator.java
    │   ├── fsm/events/Amqp10Frame.java
    │   ├── codec/Amqp10TypeReader.java
    │   ├── codec/Amqp10TypeWriter.java
    │   ├── codec/DescribedType.java
    │   ├── codec/AmqpSymbol.java
    │   ├── codec/UnsignedByte.java  UnsignedShort.java  UnsignedInt.java  UnsignedLong.java
    │   ├── codec/Amqp10Binary.java
    │   ├── messages/Amqp10BaseFrame.java
    │   ├── messages/GenericFrame.java
    │   ├── messages/EmptyFrame.java               (keep-alive / heartbeat)
    │   ├── messages/performatives/Open.java Begin.java Attach.java Flow.java
    │   │                          Transfer.java Disposition.java Detach.java
    │   │                          End.java Close.java
    │   ├── messages/sasl/SaslMechanisms.java SaslInit.java SaslOutcome.java
    │   │                 (SaslChallenge/SaslResponse only if a mechanism needs them)
    │   ├── messages/sections/Header.java DeliveryAnnotations.java MessageAnnotations.java
    │   │                     Properties.java ApplicationProperties.java Data.java
    │   │                     AmqpValue.java AmqpSequence.java Footer.java
    │   ├── utils/Amqp10ProxySocket.java
    │   ├── utils/ProxyedBehaviour.java
    │   ├── utils/ReceiverLink.java                (consume-correlation DTO)
    │   └── plugins/
    │       ├── Amqp10RecordPlugin.java  Amqp10ReplayPlugin.java
    │       ├── Amqp10PublishPlugin.java Amqp10ReportPlugin.java
    │       ├── Amqp10LatencyPlugin.java Amqp10NetErrorPlugin.java
    │       ├── Amqp10RestPluginsPlugin.java
    │       ├── apis/Amqp10PublishPluginApis.java  apis/dtos/*.java
    │       └── cli/Amqp10PublishPluginCli.java  Amqp10RecordPluginCli.java
    │                Amqp10ReplayPluginCli.java  Amqp10ReportPluginCli.java
    ├── main/resources/jte/amqp10/record_plugin/index.jte
    ├── main/resources/jte/amqp10/publish_plugin/index.jte  connections.jte
    ├── test/java/org/kendar/amqp/v10/Amqp10BasicTest.java  CodecTest.java
    │                                  SimpleTest.java  ReplayerTest.java  SpecialErrorsTest.java
    └── test/resources/logback.xml   (+ recorded scenarios test*/scenario/ as they get produced)
```

## 5. Wire-protocol design

### 5.1 Descriptor codes (`dtos/Performatives`)

AMQP layer: open `0x10`, begin `0x11`, attach `0x12`, flow `0x13`, transfer `0x14`, disposition `0x15`, detach `0x16`, end `0x17`, close `0x18`.
SASL layer: sasl-mechanisms `0x40`, sasl-init `0x41`, sasl-challenge `0x42`, sasl-response `0x43`, sasl-outcome `0x44`.
Message sections: header `0x70`, delivery-annotations `0x71`, message-annotations `0x72`, properties `0x73`, application-properties `0x74`, data `0x75`, amqp-sequence `0x76`, amqp-value `0x77`, footer `0x78`.

### 5.2 Frame translator (`fsm/Amqp10FrameTranslator`)

Interrupt state consuming `BytesEvent`, mirrors v09 `AmqpFrameTranslator` but with the 1.0 envelope:

```java
public boolean canRun(BytesEvent event) {
    var rb = event.getBuffer(); rb.setPosition(0);
    if (rb.size() < 8) return false;
    var head = rb.getBytes(0, 4);
    if (head[0]=='A'&&head[1]=='M'&&head[2]=='Q'&&head[3]=='P') return false; // 8-byte header, not a frame
    var size = rb.getInt();                       // big-endian, size INCLUDES these 4 bytes
    if (rb.size() < size) { rb.setPosition(0); throw new AskMoreDataException(); }
    rb.setPosition(0); return true;
}
public Iterator<ProtoStep> execute(BytesEvent event) {
    var rb = event.getBuffer();
    var size = rb.getInt(); var doff = rb.get(); var type = rb.get(); var channel = rb.getShort();
    var body = rb.getBytes(size - 8);             // includes extended header when doff > 2
    var bb = new BBuffer();                       // repackage the WHOLE raw frame → byte-exact passthrough
    bb.writeInt(size); bb.write(doff); bb.write(type); bb.writeShort(channel); bb.write(body);
    bb.setPosition(0);
    event.getContext().send(new Amqp10Frame(event.getContext(), event.getPrevState(), bb, channel, type));
    return iteratorOfEmpty();
}
```

`messages/Amqp10BaseFrame.write()` uses the v09 back-patch trick: write placeholder size, DOFF=2, type, channel, `writeFrameContent(rb)`, then patch the size at position 0. **No trailing `0xCE`.** `canRunFrame` peeks the described-type descriptor after the 8-byte envelope and compares it to `getDescriptorCode()`.

`messages/EmptyFrame`: the AMQP 1.0 heartbeat is an 8-byte frame with empty body — replaces v09 `HearthBeatFrame`; also registered as an interrupt state.

### 5.3 SASL — the proxy terminates SASL (credential-substitution, the v09 model)

`fsm/ProtocolHeader` accepts either header:

- Client sends `AMQP\x03\x01\x00\x00` (SASL): the proxy answers the header + `sasl-mechanisms [PLAIN, ANONYMOUS]`, accepts the client's `sasl-init`, replies `sasl-outcome(code=0)`, then expects the client's second header `AMQP\x00\x01\x00\x00`. Toward the real broker it independently runs the SASL exchange with the **proxy's configured login/password** using `proxy.sendBytesAndExpect(...)` / `sendAndExpect(...)` chains — exactly how v09's `ProtocolHeader` drives Connection.Start/StartOk.
- Client sends `AMQP\x00\x01\x00\x00` directly: echo header downstream, plain header upstream.

Rationale: identical to v09's model (client credentials replaced by proxy credentials), and mandatory for replay-without-broker. Transparent SASL passthrough is rejected — it breaks credential substitution and replay.

FSM start: `ProtocolHeader(BytesEvent)` → optional `SaslInit(Amqp10Frame)` state (when SASL was requested; context flag `"SASL_DONE"`) → optional second `ProtocolHeader` → `Open`. Verify the exact optional/switch-case composition available in `org.kendar.protocol.states.special` as used by v09.

### 5.4 FSM wiring (`Amqp10Protocol.initializeProtocol()`)

The engine already supports multiplexing: `ProtoContext` keeps a **separate execution stack per tag value** (`executionStack` keyed by the event's tag, `ProtoContext.java:397-403`), and v09's `AmqpFrame` tags each event `CHANNEL:<n>` — that is how 0.9.1 runs an independent `ChannelOpen → while(ops) → ChannelClose` sub-FSM per channel with interleaved frames. AMQP 1.0 sessions map 1:1 onto this (one session per channel), so the **default layout uses `Tagged` like v09**:

```java
addInterruptState(new EmptyFrame(Amqp10Frame.class));
addInterruptState(new Amqp10FrameTranslator(BytesEvent.class));
initialize(new ProtoStateSequence(
    new ProtocolHeader(BytesEvent.class),
    new SaslInit(Amqp10Frame.class),               // optional (SASL path)
    new Open(Amqp10Frame.class),
    new Tagged(Tag.ofKeys("SESSION"),
        new ProtoStateWhile(new ProtoStateSequence(
            new Begin(Amqp10Frame.class),
            new ProtoStateWhile(new ProtoStateSwitchCase(
                new Attach(Amqp10Frame.class),
                new Flow(Amqp10Frame.class),
                new Transfer(Amqp10Frame.class),
                new Disposition(Amqp10Frame.class),
                new Detach(Amqp10Frame.class),
                new EmptyFrame(Amqp10Frame.class))),
            new End(Amqp10Frame.class)))),
    new Close(Amqp10Frame.class)));
```

Three 1.0-specific rules that 0.9.1 never exercises:

1. **Tag by frame kind, not `channel > 0`.** v09's `AmqpFrame` only tags events when `channel > 0` (channel 0 is connection-reserved in 0.9.1). In 1.0, clients (qpid-jms included) may legally `begin` their first session **on channel 0**, while `open`/`close` also travel on channel 0. Copying the `channel > 0` rule verbatim would drop a channel-0 session into the untagged root branch and break routing. `Amqp10Frame` must tag session performatives (begin/attach/flow/transfer/disposition/detach/end) with `SESSION:<channel>` always, and never tag `open`/`close`/SASL/empty frames.
2. **Links (handles) are a second multiplexing level inside a session** — no nested `Tagged` needed. Keep the per-session loop a flat switch-case; link state lives in the context maps (`handle → link`). Each performative frame is then handled independently and link interleaving is a non-issue.
3. **`Transfer` must be a single repeatable state, never a `ProtoStateSequence`.** The 1.0 analog of v09's `BasicPublish → HeaderFrame → BodyFrame` sequence is a multi-frame delivery (`transfer` with `more=true`), but fragments of one delivery may legally interleave with transfers on *other* links of the same session. Aggregate fragments by handle + delivery-id in the context instead.

Fallback: if `Tagged` routing misbehaves in M1, flatten to a single `ProtoStateWhile(ProtoStateSwitchCase(...))` after `Open` (all performatives incl. Begin/End in one loop, untagged) — loses per-session FSM structure but keeps passthrough and recording working.

### 5.5 Type codec (`codec/`)

The 1.0 type system is recursive and self-describing — v09's static `FieldsReader`/`FieldsWriter` approach does not fit. Design:

- `Amqp10TypeReader.readAny(BBuffer)`: switch on format code — described `0x00` (recurse for descriptor + value), fixed widths (null `0x40`, true/false `0x41/0x42`, ubyte `0x50`, uint0 `0x43`/smalluint `0x52`/uint `0x70`, ulong variants `0x44/0x53/0x80`, int/long/short/byte, float `0x72`, double `0x82`, timestamp `0x83`, uuid `0x98`, char `0x73`), variable (vbin8/32 `0xA0/0xB0`, str8/32-utf8 `0xA1/0xB1`, sym8/32 `0xA3/0xB3`), compound (list0 `0x45`, list8/32 `0xC0/0xD0`, map8/32 `0xC1/0xD1`), arrays (`0xE0/0xF0`).
- `Amqp10TypeWriter`: typed writers choosing the smallest encoding; `writeDescribed(rb, ulongCode, List<Object> fields)` with trailing-null truncation.
- `DescribedType` DTO (descriptor ulong-or-symbol + value); wrappers `AmqpSymbol`, `UnsignedByte/Short/Int/Long`, `Amqp10Binary` where Java types are ambiguous — **Jackson-friendly** (`@JsonValue`/simple getters) because recordings serialize to JSON via `JsonMapper`.
- Performatives map fields positionally per spec (e.g. attach: name=0, handle=1, role=2, …, source=5, target=6).

Like v09, most states forward raw bytes; semantic decode is required for SASL, `open` (idle-time-out), `attach`/`transfer` correlation fields, and the publish plugin's message sections. The codec is built complete in M2 regardless, since full parity includes publishing.

### 5.6 Proxy socket and async broker traffic

`utils/Amqp10ProxySocket extends NettyProxySocket` — three overrides (pattern: v09 `AmqpProxySocket`):

- `getStateToRetrieveOneSingleMessage()` → `new GenericFrame()` (splits the inbound stream on the 4-byte size prefix; must also pass 8-byte protocol headers through).
- `availableStates()` → `.asProxy()` instances of the frames the broker can initiate: **`Transfer`** (deliveries to consumers — the critical async path), `Flow`, `Disposition`, `Attach`, `Detach`, `End`, `Close`, `EmptyFrame`.
- `buildPossibleEvents(...)` → `List.of(new Amqp10Frame(context, null, buffer, (short) -1, type))`.

`utils/ProxyedBehaviour` adapted from v09: decides `proxy.respond(...)` (server push → recorded as RESPONSE) vs `sendAndForget`. Correlation: on a receiver-role `Attach`, store a `ReceiverLink` (link name, handle, source address) under context key `"ATTACH_RECEIVER_H_" + handle`, so `Transfer` pushes are tagged with their consuming link — the analog of v09's `BASIC_CONSUME_CH_` + `consumeOrigin`.

Heartbeat: copy the v09 `TimerService` loop but emit `EmptyFrame` and honor the `idle-time-out` negotiated in `open`.

## 6. Plugins (full set, tag `"amqp10"`)

| Plugin | Base | Notes |
|---|---|---|
| `Amqp10RecordPlugin` | `BasicRecordPlugin<BasicAysncRecordPluginSettings>` | `toAvoid`: `byte[]`, handshake performatives, sender-direction frames. `buildTag`: Attach source address + handle; Transfer keyed by originating link |
| `Amqp10ReplayPlugin` | `BasicReplayPlugin<BasicAysncReplayPluginSettings>` | async replay of `Transfer` pushes by tag (mirror v09) |
| `Amqp10PublishPlugin` | `ProtocolPluginDescriptorBase<PluginSettings>` | inject `Transfer` + message sections to connected receiver links; REST APIs in `plugins/apis/`, JTE UI |
| `Amqp10ReportPlugin` | as v09 | verbatim copy, retagged |
| `Amqp10LatencyPlugin` | as v09 | verbatim copy, retagged |
| `Amqp10NetErrorPlugin` | as v09 | verbatim copy, retagged |
| `Amqp10RestPluginsPlugin` | as v09 | verbatim copy, retagged |

Plus the four `plugins/cli/*` classes and JTE panels (`jte/amqp10/record_plugin/index.jte`, `jte/amqp10/publish_plugin/{index,connections}.jte`) copied from v09 with ids changed to `amqp10`.

## 7. Test infrastructure (ActiveMQ Artemis)

- **New image class** `protocol-test/src/main/java/org/kendar/tests/testcontainer/images/ArtemisImage.java`, modeled on `RabbitMqImage`: `GenericContainer` on `apache/activemq-artemis:latest-alpine`, env `ARTEMIS_USER` / `ARTEMIS_PASSWORD`, expose 5672, wait for AMQP port + retry loop like `AmqpBasicTest.beforeClassBase`. `RabbitMqImage` is left untouched (v09 tests keep it).
- **Test client**: `org.apache.qpid:qpid-jms-client` (test scope) — mainstream AMQP 1.0 JMS client; exercises SASL PLAIN and the full performative set via `amqp://localhost:5692`. Version property `qpid.jms.version` in the root pom.
- Tests (scaffold pattern: `protocol-amqp-091/src/test/java/org/kendar/amqp/v09/AmqpBasicTest.java` — manual instantiation of protocol/proxy/plugins/`NettyServer`, no DI):
  - `Amqp10BasicTest` — container + proxy scaffold, fake port 5692, `FileStorageRepository` under `target/tests/{Class}/{method}`.
  - `CodecTest` — pure `BBuffer` round-trips for every format code + Jackson JSON round-trip of wrapper types (no container).
  - `SimpleTest` — JMS produce/consume through the proxy against Artemis; assert recorded scenario files.
  - `ReplayerTest` — broker-less replay from a committed scenario (`NullStorageRepository` pattern from v09).
  - `SpecialErrorsTest` — latency / net-error plugins.

## 8. Files to modify outside the new module

| File | Change |
|---|---|
| `pom.xml` (root) | add `<module>protocol-amqp-10</module>` to **both** dev (~l.176) and deploy (~l.198) profiles; `qpid.jms.version` property |
| `protocol-runner/pom.xml` | dependency on `protocol-amqp-10` next to `protocol-amqp-091` (~l.71) |
| `jacoco/pom.xml` | add module to the three filesets (exec data ~l.68, sources ~l.104, classes ~l.133) |
| `protocol-test` | new `ArtemisImage.java` (§7) |
| `README.md` | add AMQP 1.0 to protocol list (l.7) and features/help sections |
| `settings.json` / `settings_rabbit.json` | sample block `"amqp10-01": {"protocol": "amqp10", "port": 5673, ...}` |
| `protocol-runner/src/test/.../HelpRunnerTest.java` | assert help lists `amqp10` |
| `protocol-runner/src/test/resources/*.json` (`apitest.json`, `uitest.json`, …) + `UiTest.java` | add `amqp10-01` instance where `amqp091-01` appears |
| `sample-plugins` | `Amqp10Filter` example (parity with `Amqp091Filter`) |
| `docs/` | protocol page if the per-protocol doc pattern applies; module `README.md` always |

## 9. Milestones

1. **M1 — Skeleton + passthrough**: module pom + registration in root pom, settings/protocol/context/proxy/proxy-socket, frame translator, `ProtocolHeader` with SASL termination, `GenericFrame`, performatives as raw-forwarding states, `ArtemisImage`.
   *Gate: a qpid-jms client sends and receives through the proxy to Artemis.*
2. **M2 — Codec** ✅ (completed 2026-08-07): `Amqp10TypeReader/Writer` (all format codes incl. decimals and arrays), `DescribedType` + wrappers (`AmqpSymbol`, `Unsigned*`, `Amqp10Binary`, `AmqpChar`, `AmqpTimestamp`), performative descriptor codes + field decode via `codec/Amqp10Frames`. Deviation from §4: message sections are built/parsed through the codec (`writeDescribed` + `Performatives` section constants) instead of dedicated `messages/sections/*` classes.
   *Gate: `CodecTest` green including JSON serialization round-trip.* — **8/8 tests pass.**
3. **M3 — Record/replay**: `ProxyedBehaviour` + `ReceiverLink` correlation, Record/Replay/Report plugins.
   *Gate: `SimpleTest` and `ReplayerTest` green (replay works with no broker running).*
4. **M4 — Full parity**: Publish plugin + REST APIs + JTE UI, Latency/NetError/RestPlugins, plugin CLIs, `Amqp10CommandLineHandler`, `Amqp10JteResolver`, runner/jacoco/README/docs/sample-settings integration, `SpecialErrorsTest`, module README.
   *Gate: full build + runner UI shows and drives the amqp10 instance.*

## 9-bis. Milestone M5 — Readable recordings ✅ (completed 2026-08-07; superseded decode-on-save with decoded-primary, see below)

Recordings currently serialize `RawFrame` as opaque base64 (`"raw": "AAAA..."`) because the
M1 byte-exact-passthrough design never decodes performative fields. This milestone adds a
**derived readable JSON view next to the raw bytes** — never replacing them. Replay keeps
reconstructing frames exclusively from `raw` (`Amqp10ReplayPlugin.toRawFrame` already reads
only `raw`/`frameType`/`channel`, so extra JSON properties are ignored ⇒ zero replay risk,
and old scenario files without the new fields stay valid).

1. **`codec/Amqp10FrameDescriber`** (new): `describe(byte[] frame) → Map<String,Object>` built
   on `Amqp10TypeReader` / `Amqp10Frames`:
   - envelope: `frameKind` (`AMQP`/`SASL`), `channel`, `doff` (only when > 2);
   - `performative`: symbolic name from the descriptor code (`open`, `begin`, `attach`, `flow`,
     `transfer`, `disposition`, `detach`, `end`, `close`, `sasl-*`, or `empty` for heartbeats);
   - `fields`: positional list → named map per spec (open: container-id, hostname, max-frame-size,
     channel-max, idle-time-out…; begin: remote-channel, next-outgoing-id, incoming-window,
     outgoing-window…; attach: name, handle, role, snd-settle-mode, rcv-settle-mode, source,
     target…; flow: next-incoming-id, incoming-window, next-outgoing-id, outgoing-window, handle,
     delivery-count, link-credit…; transfer: handle, delivery-id, delivery-tag, message-format,
     settled, more…; disposition: role, first, last, settled, state; detach/end/close: … + error).
     Trailing-null truncation means absent fields are simply omitted;
   - for `transfer`: decode the remaining body as message `sections` (header,
     delivery-annotations, message-annotations, properties [named per spec: message-id, to,
     subject, reply-to, correlation-id, content-type…], application-properties,
     data [base64 + `utf8` preview when printable], amqp-value, amqp-sequence, footer);
   - value rendering: wrappers serialize via their existing `@JsonValue`; `source`/`target`
     described types recursively expanded with their own field names; anything undecodable
     falls back to `{"descriptor": "0x…", "value": …}`. Decoder must be total: any exception
     → `"decoded": null`, never a failed recording.
2. **`RawFrame.getDecoded()`**: lazy, Jackson-visible read-only property
   (`@JsonProperty(access = READ_ONLY)` or getter-without-setter) calling the describer on
   `raw`. This is the minimal integration point — `StorageItem` serializes the frame object
   itself, so recordings gain `"decoded": {…}` automatically for input and output.
3. **Index readability**: in `Amqp10RecordPlugin.buildTag`, add `"performative"` (from the
   describer) and, for attach/transfer, the source/target address — the v09 `consumeOrigin`
   analog. Do **not** change `inputType`/`outputType` (`"RawFrame"`): the replay plugin
   matches on `output=RawFrame` tags.
4. **Tests**: new `FrameDescribeTest` (no container) — feed the committed
   `test/resources/replay_open/scenario/*.json` raw payloads through the describer and assert
   performative names + key fields; assert Jackson round-trip ignores `decoded` on
   deserialize; `ReplayerTest` must stay green against the *existing* (undecoded) scenario
   files to prove backward compatibility. Re-record one scenario to commit a readable example.
5. **Docs**: module README — recording format section showing a decoded transfer example;
   note that `decoded` is informational and `raw` is authoritative.

Explicitly rejected: replacing `raw` with typed frame classes re-encoded at replay (the v09
model). It forfeits byte-exactness (delivery-ids, flow credit, vendor extensions, DOFF>2)
for no functional gain — readability only needs the one-way decode above.

*Gate: recordings show named performatives/fields/sections; `FrameDescribeTest` green;
`ReplayerTest` green on pre-existing raw-only scenarios.* — **Met and extended: the final
implementation goes further than planned — recordings store ONLY the readable `decoded`
JSON (no raw blob), and replay re-encodes wire frames from it via the schema-driven
`Amqp10FrameEncoder` (shared `Amqp10Schema` keeps describer/encoder symmetric). A `raw`
base64 fallback is written only when describe→encode→describe fails to round-trip, and
always wins on replay, keeping pre-M5 raw-only scenarios valid. Untyped values (map
values, amqp-value, message-id) wrap ambiguous scalars as `{"type":"byte","value":5}` to
preserve exact wire types (qpid casts JMS annotations to Byte). Also fixed a shared
`FileStorageRepository` replay-init sort: same-millisecond index lines now tie-break by
index, otherwise a leading RESPONSE could be trimmed at replay activation.
Verified: FrameDescribeTest 9/9, ReplayerTest 5/5 (incl. full produce+consume replayed to
a live qpid-jms client purely from readable JSON via the committed `replay_readable`
fixture), RecordTest/SimpleTest/SpecialErrorsTest against Artemis, protocol-common
106/106, v09 ReplayerTest 3/3.**

## 10. Verification

- `mvn -pl protocol-amqp-10 -am test` — codec + container tests (Docker required).
- `mvn install` at root — module registration in both profiles, runner packaging.
- End-to-end: run `protocol-runner` with a settings file containing an `amqp10` instance; connect qpid-jms producer/consumer through it against Artemis; record then replay from the UI panels.
- `HelpRunnerTest` / `UiTest` confirm CLI and UI registration.

## 11. Risks

1. **FSM shape**: session multiplexing is covered by the engine's per-tag execution stacks exactly as for 0.9.1 channels (verified in `ProtoContext`). The residual risks are the three 1.0-specific rules in §5.4 — channel-0 sessions (tag by frame kind, not `channel > 0`), link-level interleaving (flat per-session loop, link state in context), and interleaved multi-frame deliveries (`Transfer` as repeatable state, never a sequence). Fallback: flatten to an untagged while/switch-case.
2. **Pipelined broker frames**: Artemis may pipeline `open` right after the SASL outcome + header; `sendBytesAndExpect` + the splitter must tolerate multiple frames per read — verify `NettyProxySocket.read` behavior early in M1.
3. **Jackson serialization** of codec wrappers (UnsignedInt, Symbol, Binary) — recordings are JSON; covered by the M2 round-trip test.
4. **Artemis container defaults** — confirm image tag, `ARTEMIS_USER/PASSWORD` env behavior and anonymous-vs-PLAIN auth during M1.
5. **DOFF > 2 extended headers** — rare; handled by forwarding raw frame bytes untouched.
6. **Heartbeat/idle-timeout** — must respect the `idle-time-out` negotiated in `open`, else brokers drop idle proxied connections mid-test.
