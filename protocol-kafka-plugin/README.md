# protocol-kafka-plugin

Apache Kafka wire-protocol support for The Protocol Master, shipped as a
self-contained PF4J **plugin jar** (dropped into `target/plugins`, per
[protocol.pluginization.md](../protocol.pluginization.md)). The protocol logic
itself is a record/replay proxy following the `protocol-amqp-10` proxy style
(see [protocol-kafka.md](../protocol-kafka.md) for the full design).

## What it does

The proxy sits in front of a Kafka broker; clients bootstrap to the proxy and,
because the proxy rewrites the broker addresses Kafka hands back, they keep
talking only to the proxy — so all traffic can be recorded, replayed, delayed,
faulted, or produced into.

Kafka is strictly client-initiated request/response with in-order responses per
connection and **no server push**, so the FSM is flat: a size-prefixed frame
translator feeds a `while(switch-case)` of request states, each forwarding the
raw request to the broker and expecting a correlation-id-matched response.

### The three Kafka-specific pillars

1. **Advertised-address rewrite** — `Metadata` (3), `FindCoordinator` (10) and
   `DescribeCluster` (60) responses have every broker/coordinator `host:port`
   rewritten to the proxy's own `advertisedHost` (setting, default `localhost`)
   + bind port, so clients never bypass the proxy.
2. **Version capping** — the `ApiVersions` (18) response is intercepted and each
   key's max version capped to `min(brokerMax, ourMax)` (Metadata ≤ 12,
   Fetch ≤ 12, Produce ≤ 9, FindCoordinator ≤ 4) so clients never negotiate an
   encoding we cannot decode.
3. **Generic passthrough + semantic decode only where needed** — most of the ~70
   APIs are forwarded as opaque raw frames; only the APIs above (plus Produce for
   the publish plugin) are decoded. Tagged fields and record batches are always
   opaque (CRC32C never recomputed) except in the publish encoder.

## Settings

| Field | Meaning | Default |
|---|---|---|
| `port` | proxy bind port | 9092 |
| `connectionString` | real broker, `tcp://host:port` | — |
| `advertisedHost` | host rewritten into Metadata/coordinator responses | `localhost` |
| `login` / `password` | reserved (SASL not yet wired) | — |

See [`settings_kafka.json`](../settings_kafka.json) for a full sample.

## Plugins

`record`, `replay`, `publish` (real produce through the proxy connection),
`report`, `latency`, `net-error`, `rest-plugins` — parity with the other
protocol modules.

## Build & test

```
mvn -pl protocol-kafka-plugin -am -Pdev install     # builds target/plugins/protocol-kafka-plugin.jar
mvn -pl protocol-kafka-plugin -Pdev test            # CodecTest (no Docker) + SimpleTest/RecordTest (Docker)
```

Tests use a single-node `apache/kafka` KRaft testcontainer and drive a real
`kafka-clients` producer/consumer through the proxy on port 9192.
