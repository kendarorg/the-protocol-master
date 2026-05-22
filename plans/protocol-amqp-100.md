# AMQP 1.0 Protocol Implementation Plan

## Overview

New module `protocol-amqp-100`, package `org.kendar.amqp.v10`. Mirrors the structure of `protocol-amqp-091` but AMQP 1.0 is architecturally different enough that very little code is reused.

**Key differences from 0.9.1:**
- Frame header is 8 bytes: `[size:4][doff:1][type:1][channel:2]` — no frame-end byte
- Completely different binary codec (self-describing type system with format codes)
- SASL is a separate framing layer before AMQP frames (different magic header)
- Sessions (= channels) contain Links; Links carry messages via Transfer frames
- 9 performatives instead of class/method pairs
- Flow control is credit-based per-Link, not per-Channel

---

## Step 0 — Maven module

**Files:**
- `protocol-amqp-100/pom.xml` — standard module pom, depends on `protocol-common`
- Root `pom.xml` — add `protocol-amqp-100` to `<modules>`
- `protocol-runner/pom.xml` — add runtime dependency

---

## Step 1 — AMQP 1.0 Type System Codec (hardest part)

AMQP 1.0 uses a self-describing binary encoding. Every value is prefixed by a 1-byte format code.

### Format code categories

| Range | Category |
|---|---|
| `0x40–0x4F` | fixed-zero (null, bool-true, bool-false, uint0, ulong0, list0) |
| `0x50–0x5F` | fixed-1 byte |
| `0x60–0x6F` | fixed-2 bytes |
| `0x70–0x7F` | fixed-4 bytes |
| `0x80–0x8F` | fixed-8 bytes |
| `0x90–0x9F` | fixed-16 bytes |
| `0xA0–0xAF` | variable-1 (1-byte size prefix) |
| `0xB0–0xBF` | variable-4 (4-byte size prefix) |
| `0xC0–0xCF` | compound-1 (1-byte size + 1-byte count) |
| `0xD0–0xDF` | compound-4 (4-byte size + 4-byte count) |
| `0xE0–0xEF` | array-1 |
| `0xF0–0xFF` | array-4 |
| `0x00` | described type constructor |

### Key format codes

| Code | Type |
|---|---|
| `0x40` | null |
| `0x41` | boolean true |
| `0x42` | boolean false |
| `0x43` | uint 0 |
| `0x44` | ulong 0 |
| `0x45` | list (empty) |
| `0x50` | ubyte |
| `0x51` | byte |
| `0x52` | uint (small, 1 byte) |
| `0x53` | ulong (small, 1 byte) |
| `0x54` | int (small, 1 byte) |
| `0x55` | long (small, 1 byte) |
| `0x56` | boolean (1 byte, 0=false) |
| `0x60` | ushort |
| `0x61` | short |
| `0x70` | uint (4 bytes) |
| `0x71` | int (4 bytes) |
| `0x72` | float |
| `0x73` | char (UTF-32BE) |
| `0x74` | decimal32 |
| `0x80` | ulong (8 bytes) |
| `0x81` | long (8 bytes) |
| `0x82` | double |
| `0x83` | timestamp (ms since epoch, int64) |
| `0x84` | decimal64 |
| `0x94` | decimal128 |
| `0x98` | uuid (16 bytes) |
| `0xa0` | vbin8 (binary, 1-byte size) |
| `0xa1` | str8-utf8 |
| `0xa3` | sym8 (symbol) |
| `0xb0` | vbin32 |
| `0xb1` | str32-utf8 |
| `0xb3` | sym32 |
| `0xc0` | list8 |
| `0xc1` | map8 |
| `0xd0` | list32 |
| `0xd1` | map32 |
| `0xe0` | array8 |
| `0xf0` | array32 |

### Described types

Descriptor format: `0x00` `<descriptor>` `<value>`

Descriptor can be a `ulong` (numeric) or a `symbol` (domain:name). Performatives and message sections use numeric descriptors.

### Classes to build

```
codec/
  AmqpTypeReader   — reads any AMQP value from BBuffer, returns Object (Long, String, List, Map, etc.)
  AmqpTypeWriter   — writes any Java value to BBuffer with correct format code
  AmqpDescribed    — wrapper: descriptor(ulong) + value(Object)
```

`AmqpTypeReader.readAny(BBuffer)` is the recursive entry point. Described type: read `0x00`, read descriptor (ulong or symbol), read value.

`AmqpTypeWriter.writeAny(BBuffer, Object)` picks the most compact format code for the Java type.

---

## Step 2 — Frame model and translator

### Frame header (8 bytes, big-endian)

```
[SIZE:uint32][DOFF:uint8][TYPE:uint8][CHANNEL:uint16]
```

- `DOFF` × 4 = offset to body start (minimum 2 → 8-byte header, no extended header)
- `TYPE = 0x00` → AMQP frame
- `TYPE = 0x01` → SASL frame
- Empty frame (no body) = heartbeat/keepalive

### Classes

```
frames/
  Amqp10FrameHeader   — parsed header (size, doff, type, channel)
  Amqp10Frame         — event wrapping header + body BBuffer  (analogous to AmqpFrame)
```

### Translator

`Amqp10FrameTranslator extends ProtoState implements InterruptProtoState`:
1. Skip if buffer starts with `AMQP` (goes to header handlers).
2. Need ≥ 8 bytes; read SIZE; need `SIZE` total bytes. Throw `AskMoreDataException` if short.
3. Parse 8-byte header, extract body bytes.
4. Dispatch: `context.send(new Amqp10Frame(...))`.

Works for both TYPE=0x00 and TYPE=0x01 — downstream states discriminate by type.

---

## Step 3 — Protocol headers

Two magic headers must be detected from `BytesEvent`:

### SASL header
`AMQP\x03\x01\x00\x00`

`SaslProtocolHeader` → detects magic, sends magic back to proxy, sends `SaslMechanisms` to client.

### AMQP header
`AMQP\x00\x01\x00\x00`

`Amqp10ProtocolHeader` → detects magic, forwards to proxy, sends `AmqpOpen` from server.

Both states check `canRun(BytesEvent)` against exact 8-byte magic.

---

## Step 4 — SASL performatives

Sent on SASL frames (TYPE=0x01, channel=0). Body is a described list.

| Descriptor | Performative | Direction | Fields |
|---|---|---|---|
| `0x40` | SaslMechanisms | S→C | mechanisms: symbol[] |
| `0x41` | SaslInit | C→S | mechanism: symbol, initial-response: binary, hostname: string |
| `0x42` | SaslChallenge | S→C | challenge: binary |
| `0x43` | SaslResponse | C→S | response: binary |
| `0x44` | SaslOutcome | S→C | code: ubyte (0=ok), additional-data: binary |

Classes: `sasl/SaslMechanisms`, `SaslInit`, `SaslChallenge`, `SaslResponse`, `SaslOutcome`

All extend `Amqp10SaslFrame` (abstract, handles TYPE=0x01 encoding).

SASL flow:
```
Client → Proxy:  AMQP\x03\x01\x00\x00
Proxy  → Client: SaslMechanisms [PLAIN, ANONYMOUS]
Client → Proxy:  SaslInit (PLAIN, \0user\0pass)
Proxy  → real:   relay SASL exchange
Proxy  → Client: SaslOutcome (code=0)
```

After `SaslOutcome(code=0)`, the SASL layer ends. Client sends AMQP header next.

---

## Step 5 — AMQP connection performatives

Sent on AMQP frames (TYPE=0x00, channel=0).

| Descriptor | Performative | Fields |
|---|---|---|
| `0x10` | Open | container-id*, hostname, max-frame-size, channel-max, idle-time-out, outgoing-locales, incoming-locales, offered-capabilities, desired-capabilities, properties |
| `0x18` | Close | error? |

`Open` flow:
```
Client → Proxy:  Open {container-id, hostname, max-frame-size, channel-max, idle-time-out}
Proxy  → real:   Open (relay)
real   → Proxy:  Open (from server)
Proxy  → Client: Open (relayed)
```

Classes: `performatives/connection/AmqpOpen`, `AmqpClose`
Both extend `Amqp10Performative` (abstract, handles TYPE=0x00 encoding with descriptor).

---

## Step 6 — Session performatives

Sent on AMQP frames, channel = session's channel number.

| Descriptor | Performative | Fields |
|---|---|---|
| `0x11` | Begin | remote-channel, next-outgoing-id*, incoming-window*, outgoing-window*, handle-max, offered-capabilities, desired-capabilities, properties |
| `0x17` | End | error? |

Sessions use window-based flow control. Context must track per-session:
- `nextOutgoingId` (delivery sequence number)
- `nextIncomingId`
- `incomingWindow`, `outgoingWindow`

Classes: `performatives/session/AmqpBegin`, `AmqpEnd`

---

## Step 7 — Link performatives

| Descriptor | Performative | Key fields |
|---|---|---|
| `0x12` | Attach | name*, handle*, role*(sender/receiver), snd-settle-mode, rcv-settle-mode, source, target, initial-delivery-count, max-message-size |
| `0x16` | Detach | handle*, closed, error |
| `0x13` | Flow | next-incoming-id, incoming-window*, next-outgoing-id*, outgoing-window*, handle, delivery-count, link-credit, available, drain |
| `0x14` | Transfer | handle*, delivery-id, delivery-tag, message-format, settled, more, rcv-settle-mode, state, resume, aborted, batchable |
| `0x15` | Disposition | role*, first*, last, settled, state, batchable |

**Attach** creates a Link. `role=false` = sender, `role=true` = receiver.

**Transfer** carries message data. The Transfer frame body contains the performative list followed directly by message section bytes (Header, Properties, ApplicationProperties, Data sections — each a described type). A message spanning multiple frames has `more=true` until the last fragment.

**Flow** grants credit. Receiver sends Flow with `link-credit=N` to allow N more transfers.

**Disposition** settles deliveries. `state` is Accepted/Released/Rejected/Modified.

Classes: `performatives/link/AmqpAttach`, `AmqpDetach`, `AmqpFlow`, `AmqpTransfer`, `AmqpDisposition`

### Message section descriptors

| Descriptor | Section |
|---|---|
| `0x70` | Header |
| `0x71` | DeliveryAnnotations |
| `0x72` | MessageAnnotations |
| `0x73` | Properties |
| `0x74` | ApplicationProperties |
| `0x75` | Data (binary body) |
| `0x76` | AmqpSequence |
| `0x77` | AmqpValue |
| `0x78` | Footer |

Classes in `messages/sections/`: `MessageHeader`, `MessageProperties`, `ApplicationProperties`, `MessageData`, etc.

---

## Step 8 — Context

`Amqp10ProtoContext extends NetworkProtoContext`:

```java
// Connection-level
String  remoteContainerId;
int     maxFrameSize    = 65536;
int     channelMax      = 65535;
long    idleTimeoutMs   = 0;

// Session state keyed by channel
Map<Integer, SessionState>  sessions;

// per SessionState
int nextOutgoingId;
int nextIncomingId;
int incomingWindow;
int outgoingWindow;
Map<Integer, LinkState> links;  // keyed by handle

// per LinkState
String  name;
boolean senderRole;
String  sourceAddress;
String  targetAddress;
long    deliveryCount;
long    linkCredit;
```

The heartbeat timer uses `idleTimeoutMs` negotiated in `Open` (send empty frame at half the interval).

---

## Step 9 — Protocol FSM

```
Amqp10Protocol.initializeProtocol():

  addInterruptState(new Amqp10FrameTranslator(BytesEvent.class))
  addInterruptState(new EmptyFrame(Amqp10Frame.class))  // heartbeat

  initialize(
    new ProtoStateSwitchCase(                           // SASL or direct AMQP
      new ProtoStateSequence(                           // SASL path
        new SaslProtocolHeader(BytesEvent.class),
        new SaslMechanisms(Amqp10Frame.class),          // send
        new SaslInit(Amqp10Frame.class),                // receive
        new SaslOutcome(Amqp10Frame.class),             // send
        new Amqp10ProtocolHeader(BytesEvent.class)      // AMQP header after SASL
      ),
      new Amqp10ProtocolHeader(BytesEvent.class)        // no-SASL path
    ),
    new AmqpOpen(Amqp10Frame.class),                    // exchange open
    new ProtoStateWhile(                                // sessions
      new ProtoStateSequence(
        new AmqpBegin(Amqp10Frame.class),               // exchange begin
        new ProtoStateWhile(
          new ProtoStateSwitchCase(
            new AmqpAttach(Amqp10Frame.class),
            new AmqpDetach(Amqp10Frame.class),
            new AmqpTransfer(Amqp10Frame.class),
            new AmqpFlow(Amqp10Frame.class),
            new AmqpDisposition(Amqp10Frame.class),
            new EmptyFrame(Amqp10Frame.class)
          )
        ),
        new AmqpEnd(Amqp10Frame.class)
      )
    ),
    new AmqpClose(Amqp10Frame.class)
  )
```

`EmptyFrame` (size=8, no body) is both heartbeat reception and interrupt state for sending.

---

## Step 10 — Proxy and settings

`Amqp10Proxy extends NetworkProxy` — same pattern as `AmqpProxy`.

`Amqp10ProtocolSettings extends ByteProtocolSettingsWithLogin` — port (default 5672), timeoutSeconds.

`Amqp10JteResolver` — JTE template resolver for web UI.

`Amqp10CommandLineHandler` — CLI handler.

---

## Step 11 — Plugins

Mirror the 0.9.1 plugin set:

| Plugin | Notes |
|---|---|
| `Amqp10LatencyPlugin` | inject delay on Transfer frames |
| `Amqp10NetErrorPlugin` | drop/corrupt frames |
| `Amqp10RecordPlugin` | record Open/Begin/Attach/Transfer/Disposition sequences to JSON |
| `Amqp10ReplayPlugin` | replay recorded sessions |
| `Amqp10ReportPlugin` | human-readable log of all performatives |
| `Amqp10PublishPlugin` | inject Transfer frames into live sessions via REST API |
| `Amqp10RestPluginsPlugin` | REST management for all plugins |

Recording key: record the full message (all Transfer fragments reassembled) plus Attach context (source/target addresses) so replay can reconstruct the link topology.

---

## Step 12 — Tests

Use **RabbitMQ 4.x** docker image (supports AMQP 1.0 natively since 4.0).

Test cases:
1. **BasicConnect** — SASL PLAIN auth, Open/Close
2. **BasicPublish** — Attach sender link, Transfer, Detach
3. **BasicConsume** — Attach receiver link, Flow credit, Transfer delivery, Disposition(Accepted)
4. **FlowControl** — receiver grants 1 credit at a time, verify backpressure
5. **MultiSession** — two concurrent sessions on one connection
6. **Heartbeat** — idle connection stays alive via empty frames
7. **RecordReplay** — record a publish/consume cycle, replay it offline
8. **NoSasl** — connect without SASL (if broker supports it)

Test base class mirrors `AmqpBasicTest` pattern (Testcontainers + RabbitMQ 4.x image).

---

## Implementation Order

| # | Task | Dependency | Risk |
|---|---|---|---|
| 1 | Maven module | — | low |
| 2 | Type codec (Reader + Writer) | — | **HIGH** — all frames depend on this |
| 3 | Frame header + translator | — | medium |
| 4 | Protocol headers (SASL + AMQP) | 3 | low |
| 5 | SASL performatives | 2, 4 | medium |
| 6 | Open + Close | 2, 3 | low |
| 7 | Begin + End | 6 | low |
| 8 | Attach + Detach | 7 | low |
| 9 | Transfer + message sections | 8 | medium |
| 10 | Flow + Disposition | 8 | medium |
| 11 | Context | 6–10 | medium |
| 12 | FSM wiring in Amqp10Protocol | 4–11 | medium |
| 13 | Proxy + settings | 12 | low |
| 14 | Tests (connect + basic pub/con) | 13 | low |
| 15 | Plugins | 14 | low |
| 16 | Full test suite | 15 | low |

**Start with Step 2 (type codec)** — everything else is blocked on it. Write unit tests for the codec in isolation before touching any frame logic.

---

## Notable Gotchas

1. **Described type constructors are recursive**: `0x00 <descriptor> <value>` where descriptor itself can be a described type. Reader must handle arbitrary depth.

2. **Transfer fragmentation**: large messages split across frames. `more=true` means more fragments follow on same link handle. Must buffer until `more=false` before passing to plugins.

3. **Channel remapping in proxy mode**: client assigns channel N for Begin; proxy may use different channel N' to real broker. Context must maintain client↔broker channel maps.

4. **Handle remapping**: same issue for link handles per session.

5. **Settlement modes**: `snd-settle-mode` / `rcv-settle-mode` on Attach control whether Disposition frames exist. In settled mode (pre-settled) there are no Disposition frames — record plugin must account for this.

6. **SASL is optional**: some clients (JMS, .NET) skip SASL and send AMQP header directly. FSM must handle both paths (see `ProtoStateSwitchCase` in Step 9).

7. **Null fields in lists**: AMQP 1.0 performatives are encoded as lists. Trailing null fields may be omitted. Reader must handle short lists (missing = null/default).

8. **idle-time-out**: value in Open is milliseconds; send empty frames at ≤ `idle-time-out/2` interval. Zero means no timeout.
