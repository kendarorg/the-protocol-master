# protocol-amqp-10

AMQP 1.0 (OASIS / ISO 19464) protocol for The Protocol Master, packaged as a
**pf4j plugin jar** (see `../protocol.pluginization.md`) rather than a compiled-in
module. On `mvn install` the assembly is copied to `../target/plugins/protocol-amqp-10.jar`
and discovered at runtime by the runner's `JarPluginManager`.

Protocol design and milestones: see `../protocol-amqp-10.md`.

## Status

**M3 — in progress (broker-verified loop is now unblocked).** Docker/testcontainers
here negotiates Docker API 1.32 (docker-java 3.4.0 default), which modern daemons
(min 1.40) reject. Fixed for the run by passing the version as a JVM system property
to the surefire fork (docker-java reads only `-Dapi.version`, *not* `DOCKER_HOST`-style
env). Run the container tests with:

```
DOCKER_HOST=unix:///var/run/docker.sock \
  mvn -pl protocol-amqp-10 test -Dtest=SimpleTest \
      -Dsurefire.failIfNoSpecifiedTests=false -DargLine="-Dapi.version=1.51"
```

With that, Artemis starts and qpid-jms reaches the proxy. `SimpleTest` currently
fails at the **AMQP 1.0 SASL handshake**: the proxy must terminate SASL on both
sides (send `sasl-mechanisms` to the client + `sasl-outcome`, and run PLAIN with the
broker using the proxy's credentials) before `open`/sessions. That bidirectional
handshake — built from the M2 codec — is the next implementation step. The header
forwarding no longer crashes (a `GenericFrame`-as-expect-state `ClassCastException`
was fixed).


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
