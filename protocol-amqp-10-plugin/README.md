# protocol-amqp-10

AMQP 1.0 (OASIS / ISO 19464) protocol for The Protocol Master, packaged as a
**pf4j plugin jar** (see `../protocol.pluginization.md`) rather than a compiled-in
module. On `mvn install` the assembly is copied to `../target/plugins/protocol-amqp-10.jar`
and discovered at runtime by the runner's `JarPluginManager`.

Protocol design and milestones: see `../protocol-amqp-10.md`.

## Recording format (M5 — readable recordings)

Recorded frames store the **readable JSON as the primary representation**: the
`decoded` tree produced by `codec/Amqp10FrameDescriber` (performative name,
spec-named fields from the shared `codec/Amqp10Schema`, and the decoded message
sections for `transfer`). On replay, `codec/Amqp10FrameEncoder` rebuilds the wire
frame from that tree — semantically equivalent bytes, verified end to end against
a live qpid-jms client (`ReplayerTest.replaysReadableScenarioWithoutBroker`).

```json
{
  "decoded": {
    "frameKind": "AMQP", "channel": 1, "performative": "transfer",
    "fields": {"handle": 0, "delivery-id": 0, "delivery-tag": "AA==",
               "message-format": 0, "settled": false},
    "sections": [
      {"section": "message-annotations",
       "value": {"x-opt-jms-msg-type": {"type": "byte", "value": 5}}},
      {"section": "properties", "fields": {"message-id": "ID:...", "to": "my-queue"}},
      {"section": "amqp-value", "value": "hello"}
    ]
  }
}
```

Type fidelity: spec-typed fields are plain JSON scalars (the schema restores their
wire type on encode). Untyped contexts — map values, `amqp-value`, message-id — wrap
ambiguous scalars as `{"type": "byte", "value": 5}` so the exact wire type survives
(qpid's JMS layer casts `x-opt-jms-msg-type` to `Byte`, so this matters in practice).

A `raw` base64 fallback is written **only** when `describe → encode → describe` does
not round-trip (unknown descriptor, codec gap); when present it wins on replay.
Pre-M5 raw-only scenarios (e.g. `replay_open`) stay valid — `raw` is always honored.

The scenario index (`index.default.json`) tags every line with `performative` and,
for `attach`, the source/target `address` — making scenarios greppable by frame kind
and queue (the v09 `consumeOrigin` analog). Gate: `FrameDescribeTest` (no broker) +
both `ReplayerTest` fixtures (raw-only and readable).

## Status

**M3 — end-to-end passthrough works (broker-verified).** `SimpleTest` is GREEN: a
qpid-jms producer/consumer round-trips a message through the proxy to ActiveMQ
Artemis. The full AMQP 1.0 handshake and steady state relay correctly — SASL
header → `sasl-mechanisms` → `sasl-init` → `sasl-outcome` → AMQP header → `open` →
`begin` → `attach`/`flow`/`transfer`/`disposition` → clean `detach`/`end`/`close`.

Approach: **transparent SASL relay** (not termination) — client SASL frames/headers
are forwarded to the broker, and the broker's replies are relayed back by dedicated
proxy states (`HeaderRelay`, `SaslMechanisms`, `SaslOutcome`, `Open`, `Begin`, …).
Key fixes that got it green: `ProtocolHeader` forwards via `sendAndForget` and
`truncate(8)`-consumes the header; the SASL/2nd-header FSM states are `.asOptional()`
(cf. Postgres `SSLRequest`); `GenericFrame` resets position to 0 so multiple broker
frames in one packet all split; teardown performatives are interrupt states.

Run the container tests (docker-java 3.4.0 defaults to Docker API 1.32, which modern
daemons reject — pass the version as a JVM system property to the surefire fork; it
reads only `-Dapi.version`, not env):

```
DOCKER_HOST=unix:///var/run/docker.sock \
  mvn -pl protocol-amqp-10 test -Dtest=SimpleTest \
      -Dsurefire.failIfNoSpecifiedTests=false -DargLine="-Dapi.version=1.51"
```

Still to do: credential-substituting SASL termination (needed for broker-less
**replay**), record/replay plugins (`ProxyedBehaviour` + `ReceiverLink`), the
remaining plugins/CLIs/JTE panels, and `ReplayerTest`/`SpecialErrorsTest`.


**M2 — type codec (done, unit-verified).** `codec/` — `Amqp10TypeReader`/`Amqp10TypeWriter`
(all AMQP 1.0 format codes, smallest-encoding writes, `writeDescribed` with trailing-null
truncation) + Jackson-friendly wrappers (`AmqpSymbol`, `Unsigned{Byte,Short,Int,Long}`,
`Amqp10Binary`, `AmqpTimestamp`, `AmqpChar`, `DescribedType`). Gate: `CodecTest` (8 tests,
BBuffer round-trips for every code + JSON round-trip) is green — no broker needed:
`mvn -pl protocol-amqp-10 test -Dtest=CodecTest`.

**M1 — pluginized skeleton + passthrough (done, build-verified).** Compiling module wired into
DI and the runner:

- Plugin packaging: `pom.xml` (jar-with-dependencies + `Plugin-*` manifest, `provided`
  deps), `Amqp10Plugin` entry point, `protocol_amqp_10_plugin.version` resource,
  registration in the root `pom.xml` (both profiles).
- Extension classes (`@Extension` + `ExtensionPoint`, tag `amqp10`): `Amqp10Protocol`,
  `Amqp10ProtocolSettings`, `Amqp10Proxy`, `cli/Amqp10CommandLineHandler`,
  `Amqp10JteResolver`.
- Wire plumbing: `Amqp10FrameTranslator` (1.0 envelope: `size` includes itself, no
  `0xCE`), `fsm/events/Amqp10Frame` (channel + AMQP/SASL type, `SESSION:<n>` tagging
  by frame kind), `fsm/ProtocolHeader`, `messages/GenericFrame`, `messages/EmptyFrame`
  (heartbeat), `messages/Amqp10BaseFrame` + performative states
  (`open/begin/attach/flow/transfer/disposition/detach/end/close`) doing byte-exact
  passthrough, `utils/Amqp10ProxySocket`.

### Not yet done (see ../protocol-amqp-10.md)

- **Verification gate**: the header + SASL termination and `open`/session sequencing
  are wired to the §5.4 design but **not yet verified against a live broker** (needs
  Docker + ActiveMQ Artemis). Transparent vs terminated SASL is finalized here.
- **M2** — type codec (`codec/`), performative field encode/decode, message sections.
- **M3** — record/replay (`ProxyedBehaviour` + `ReceiverLink` correlation) + plugins.
- **M4** — publish/report/latency/neterror/rest plugins, CLIs, JTE panels, Artemis
  test container, qpid-jms tests, jacoco/docs/sample-settings integration.

## Build

```
mvn -pl protocol-amqp-10 -am install      # produces ../target/plugins/protocol-amqp-10.jar
```
