# Implementation Plan: `protocol-kafka` — Apache Kafka wire-protocol module

## 1. Context and goal

The Protocol Master is a multi-protocol proxy simulator (record/replay/mock of real infrastructure) built on the FSM framework in `protocol-common`. This plan adds a **`protocol-kafka`** module implementing the Apache Kafka wire protocol as a proxy with record/replay, at full parity with the existing protocol modules (all plugins, CLI, JTE UI, docs, runner integration).

Kafka differs fundamentally from the broker protocols already supported (AMQP 0.9.1, MQTT):

| Aspect | MQTT / AMQP 0.9.1 | Kafka |
|---|---|---|
| Direction | Server pushes messages to consumers | **Strictly client-initiated request/response** — consumers poll via Fetch (long-poll) |
| Framing | Protocol-specific frames | 4-byte size + payload; request header: api_key(int16), api_version(int16), correlation_id(int32), client_id |
| API surface | Fixed method set | ~70 **versioned** API keys; classic vs flexible/compact encodings (KIP-482: varints, tagged fields) chosen per version |
| Topology | Client connects to one broker | Client bootstraps to one address, then connects to brokers **advertised in Metadata responses** — a proxy must rewrite those |

Decisions confirmed:
- **Test broker: `apache/kafka-native` (KRaft single node)** via testcontainers — reference implementation, sub-second startup.
- **Scope: full parity including the publish plugin**, implemented as a *real produce through the proxy connection* (not synthetic record-batch injection).

## 2. Naming conventions

| Item | Value |
|---|---|
| Module directory / artifactId | `protocol-kafka` |
| Root package | `org.kendar.kafka` |
| Protocol id / DI tag | `"kafka"` |
| Default bind port | 9092 |
| Test fake port | **9192** (9093 is Kafka's conventional TLS/controller port — avoided) |
| JTE templates | `src/main/resources/jte/kafka/` |

## 3. The three Kafka-specific design pillars

### 3.1 Advertised-address rewrite (the critical proxy problem)
Clients bootstrap to the proxy, but `Metadata` (api key 3), `FindCoordinator` (10) and `DescribeCluster` (60) responses contain broker `host:port` lists that clients then connect to **directly**, bypassing the proxy. The proxy must decode these responses, rewrite every broker/coordinator address to its own `advertisedHost` (new settings field, default `localhost`) + own bind port, and re-encode. Single-broker assumption (matches the framework's single-connectionString `NetworkProxy`); log a warning if Metadata returns more than one broker — multi-broker is explicitly out of scope.

### 3.2 Version capping via ApiVersions interception
Intercept `ApiVersions` (18) responses and cap the advertised max version per api key to what our codec can decode (`min(brokerMax, ourMax)`) — the standard bounded-decode-surface approach used by Kafka proxies (e.g. Kroxylicious). Guarantees clients never send a semantic-API version we didn't implement. Caps: Metadata ≤ 12 (v13+ moves to topic UUIDs), Fetch ≤ 12 (same reason), Produce ≤ 9, FindCoordinator ≤ 4. Un-modeled passthrough keys keep the broker's range (only header layout matters, and that's known for all keys). NOTE: the ApiVersions **response header is always v0**, even for flexible requests.

### 3.3 Generic passthrough + semantic decode only where needed
Do **not** model ~70 APIs. Two tiers:
- **`GenericKafkaMessage`**: parsed request header + raw body bytes (base64 in JSON recordings) — every API not listed below.
- **Semantic DTOs** only for APIs needing rewrite or record-tagging: `ApiVersionsResponse`, `MetadataRequest/Response` (v1–12), `FindCoordinatorRequest/Response` (v0–4), `DescribeClusterResponse` (v0–1), `ProduceRequest` (v3–9; transactional_id + topic names only, record batches stay opaque blobs), `FetchRequest` (v4–12; topic/partition names). Group APIs (JoinGroup/SyncGroup/Heartbeat/OffsetCommit/OffsetFetch) get tiny **peek decoders** extracting only the group id (first field) for tagging — no full models.

## 4. Framework mapping (verified against the codebase)

Grounding facts:
- `BaseProxySocket.read(...)` (protocol-common/.../proxy/BaseProxySocket.java) matches queued upstream frames against the expected state via `canRunEvent`, with a **hard 2-second deadline** — relevant to Fetch long-poll (risk §10.2).
- `NetworkProxy.sendAndExpect` blocks the per-connection FSM until the response matches — Kafka guarantees in-order responses per connection, and the Java client uses a separate TCP connection for the group coordinator, so this serialization is transparent (latency only). Correlation-id matching in `canRun` makes it robust to pipelined bursts.
- **No server push in Kafka** → `availableStates()` on the proxy socket is empty, and the replay plugin has `hasCallbacks() == false` — simpler than MQTT/AMQP.
- `BBuffer` has no varint support → subclass (same pattern as `MqttBBuffer.readVarBInteger`).

Templates: `protocol-mqtt` (`MqttProtocol`, `MqttPacketTranslator`, `MqttProxySocket`) and `protocol-amqp-091` test scaffolds.

## 5. New module layout

```
protocol-kafka/
├── pom.xml                  parent protocol-master; deps: pf4j, protocol-common,
│                            protocol-test (test), org.apache.kafka:kafka-clients (test)
├── README.md
└── src/
    ├── main/java/org/kendar/kafka/
    │   ├── KafkaProtocol.java               @TpmService(tags="kafka"), extends NetworkProtoDescriptor
    │   ├── KafkaProtocolSettings.java       extends ByteProtocolSettingsWithLogin
    │   │                                    + advertisedHost field (default "localhost")
    │   ├── KafkaProxy.java                  extends NetworkProxy
    │   ├── KafkaJteResolver.java
    │   ├── cli/KafkaCommandLineHandler.java
    │   ├── context/KafkaContext.java        extends NetworkProtoContext; in-flight map
    │   │                                    correlationId → {apiKey, apiVersion, clientId}
    │   ├── enums/KafkaApiKeys.java          all ~70 key numbers + names + per-key
    │   │                                    requestHeaderVersion()/responseHeaderVersion()/flexibleFrom
    │   ├── enums/SupportedVersions.java     capping table (§3.2)
    │   ├── fsm/KafkaFrameTranslator.java    InterruptProtoState: BytesEvent → typed event;
    │   │                                    parses request header / response correlationId
    │   ├── fsm/KafkaFrame.java              NetworkProxySplitterState (4-byte size splitter)
    │   ├── fsm/events/KafkaRequestEvent.java  KafkaResponseEvent.java
    │   ├── fsm/GenericRequest.java          catch-all passthrough state (canRun always true, LAST)
    │   ├── fsm/GenericResponse.java         correlation-id-matched response, raw re-emit
    │   ├── fsm/ApiVersionsRequest.java      forward + cap response (§3.2)
    │   ├── fsm/MetadataRequest.java         forward + address rewrite (§3.1)
    │   ├── fsm/FindCoordinatorRequest.java  forward + address rewrite
    │   ├── fsm/DescribeClusterRequest.java  forward + address rewrite
    │   ├── fsm/ProduceRequest.java          semantic decode for tagging, forward
    │   ├── fsm/FetchRequest.java            semantic decode for tagging, forward
    │   ├── fsm/dtos/                        GenericKafkaMessage, MetadataResponseDto,
    │   │                                    ApiVersionsResponseDto, FindCoordinatorResponseDto,
    │   │                                    DescribeClusterResponseDto, ProduceRequestDto,
    │   │                                    FetchRequestDto, TaggedFields (opaque blob)
    │   ├── utils/KafkaBBuffer.java          extends BBuffer: varint/zigzag/compact primitives
    │   ├── utils/KafkaProxySocket.java      extends NettyProxySocket (availableStates() empty)
    │   └── plugins/
    │       ├── KafkaRecordPlugin.java       extends BasicRecordPlugin<BasicAysncRecordPluginSettings>
    │       ├── KafkaReplayPlugin.java       extends BasicReplayPlugin (hasCallbacks=false)
    │       ├── KafkaPublishPlugin.java      real produce through the proxy connection (§7)
    │       ├── KafkaReportPlugin.java  KafkaLatencyPlugin.java
    │       ├── KafkaNetErrorPlugin.java  KafkaRestPluginsPlugin.java
    │       ├── apis/KafkaPublishPluginApis.java (+dtos)
    │       └── cli/{Publish,Record,Replay,Report}PluginCli.java
    ├── main/resources/jte/kafka/record_plugin/index.jte
    ├── main/resources/jte/kafka/publish_plugin/{index,connections}.jte
    ├── test/java/org/kendar/kafka/{KafkaBasicTest,CodecTest,SimpleTest,ReplayerTest,SpecialErrorsTest}.java
    └── test/resources/logback.xml  (+ committed replay scenarios)
```

## 6. Key implementation sketches

### 6.1 Codec (`utils/KafkaBBuffer`)
```java
public class KafkaBBuffer extends BBuffer {           // always big-endian
    public int readUnsignedVarint();  public void writeUnsignedVarint(int v);   // KIP-482
    public int readVarint();          public long readVarlong();                 // zigzag
    public String readString();       // classic: int16 len, -1 = null
    public String readCompactString();// uvarint len+1, 0 = null
    public byte[] readBytes32();      public byte[] readCompactBytes();
    public int readArrayCount(boolean flexible);      // int32 vs uvarint-1
    public TaggedFields readTaggedFields();           // OPAQUE copy, never interpreted
    public UUID readUuid();
}
```
Semantic DTOs implement `read(KafkaBBuffer, short version)` / `write(KafkaBBuffer, short version)` with `flexible = version >= flexibleFrom` switching primitive families. Tagged fields and record batches are preserved as opaque byte blobs (CRC32C never recomputed).

### 6.2 Frame translator (`fsm/KafkaFrameTranslator`)
`canRun`: 4-byte size prefix; `AskMoreDataException` while incomplete. `execute`:
- **Client→proxy**: parse request header (apiKey, apiVersion, correlationId, clientId, tagged fields if header ≥ v2 per `KafkaApiKeys.requestHeaderVersion`), register in the context's in-flight map, emit `KafkaRequestEvent`.
- **Broker→proxy** (`asProxy()` instance): parse correlationId, resolve apiKey/apiVersion from the in-flight map (needed because response tagged-fields presence depends on the *request* version; exception: api key 18 response header is always v0), emit `KafkaResponseEvent`.

### 6.3 FSM wiring (`KafkaProtocol.initializeProtocol()`)
Flat — no `Tagged` composition needed (no server push, in-order responses):
```java
addInterruptState(new KafkaFrameTranslator(BytesEvent.class));
initialize(new ProtoStateWhile(new ProtoStateSwitchCase(
    new ApiVersionsRequest(KafkaRequestEvent.class),
    new MetadataRequest(KafkaRequestEvent.class),
    new FindCoordinatorRequest(KafkaRequestEvent.class),
    new DescribeClusterRequest(KafkaRequestEvent.class),
    new ProduceRequest(KafkaRequestEvent.class),
    new FetchRequest(KafkaRequestEvent.class),
    new GenericRequest(KafkaRequestEvent.class))));   // catch-all LAST
```
Request states check `event.getApiKey()` in `canRun`; each forwards via the `sendAndExpect` pattern with an expected response state whose **`canRun` matches on correlationId** (pipelining-safe). `GenericResponse` re-emits raw bytes to the client and clears the in-flight entry; the semantic response states decode → mutate (rewrite/cap) → re-encode instead.

### 6.4 Metadata rewrite (pattern for FindCoordinator/DescribeCluster too)
```java
var resp = MetadataResponseDto.read((KafkaBBuffer) event.getBuffer(), apiVersion);
for (var broker : resp.getBrokers()) {
    broker.setHost(settings.getAdvertisedHost());
    broker.setPort(settings.getPort());          // the proxy's own bind port
}
```

### 6.5 Proxy socket (`utils/KafkaProxySocket`)
- `getStateToRetrieveOneSingleMessage()` → `new KafkaFrame()` (size splitter)
- `availableStates()` → `List.of()` — **no server push in Kafka**
- `buildPossibleEvents(...)` → run the `asProxy()` translator over the buffer, return the `KafkaResponseEvent`

## 7. Plugins

| Plugin | Design |
|---|---|
| `KafkaRecordPlugin` | `buildTag`: `api` (name from KafkaApiKeys), `topic` (Produce/Fetch/Metadata), `group` (peek decoders), `partition` (Fetch). `shouldNotSave`: ApiVersions, Heartbeat, optionally empty Fetch responses (setting) to keep recordings small |
| `KafkaReplayPlugin` | `hasCallbacks() = false` (pure request/response). Replayed responses **preserve the live request's correlationId** (analog of MQTT preserving PacketIdentifier). `repeatableItems`: ApiVersions, Metadata, Heartbeat, FindCoordinator, empty Fetch, OffsetFetch. Group APIs match on api+group+topic tags (member ids/generations are volatile — never full-payload match); echo client-supplied member ids into replayed responses |
| `KafkaPublishPlugin` | **Real produce through the proxy's upstream connection** (user decision): REST API + JTE UI take topic/key/value, build a v9 ProduceRequest via the codec (one place where we do encode a record batch — single-record batch with computed CRC32C), send upstream, report the response. No synthetic Fetch injection |
| `KafkaReportPlugin`, `KafkaLatencyPlugin`, `KafkaNetErrorPlugin`, `KafkaRestPluginsPlugin` | verbatim copies from MQTT/AMQP, retagged |
| `plugins/cli/*` (4 classes), `plugins/apis/*`, JTE panels | verbatim pattern copies, ids `kafka` |

## 8. Test infrastructure

- **New** `protocol-test/src/main/java/org/kendar/tests/testcontainer/images/KafkaImage.java` extending `BaseImage<KafkaImage, org.testcontainers.kafka.KafkaContainer>` on **`apache/kafka-native:3.8.0`** (KRaft single node, no ZooKeeper). Testcontainers configures `advertised.listeners` to `localhost:<mappedPort>`; the proxy connects to `getBootstrapServers()` and the rewrite makes clients see only the proxy. Add `org.testcontainers:kafka` to protocol-test's pom.
- Test client: `org.apache.kafka:kafka-clients` (test scope). Client config in tests: `fetch.max.wait.ms=500` (under the framework's 2 s upstream-read deadline).
- Tests (scaffold copied from `protocol-amqp-091/src/test/java/org/kendar/amqp/v09/AmqpBasicTest.java` — manual wiring, `NettyServer`, fake port 9192, `FileStorageRepository` under `target/tests`):
  - `KafkaBasicTest` — container + proxy scaffold.
  - `CodecTest` — varint/compact round-trips, tagged-field opaque passthrough, **golden-bytes** decode/re-encode of captured Metadata v9/v12 + ApiVersions v3 frames (byte-identical assertion).
  - `SimpleTest` — AdminClient createTopics, produce N, consume N through the proxy (first with `enable.idempotence=false`, then with InitProducerId passthrough); assert recordings + tags.
  - `ReplayerTest` — broker-less replay from committed scenario incl. the consumer-group flow.
  - `SpecialErrorsTest` — latency / net-error plugins.

## 9. Integration files to modify

| File | Change |
|---|---|
| `pom.xml` (root) | `<module>protocol-kafka</module>` in **both** dev (~l.176) and deploy (~l.198) profiles; `kafka.clients.version` property |
| `protocol-runner/pom.xml` | dependency on `protocol-kafka` (~l.71) |
| `jacoco/pom.xml` | add module to the 3 filesets (~l.68/104/133) |
| `protocol-test` | new `KafkaImage.java` + testcontainers-kafka dep |
| `README.md` | add Kafka to protocol list and features/help sections |
| `settings.json` | sample block `"kafka-01": {"protocol": "kafka", "port": 9092, "connectionString": "tcp://localhost:9092", "advertisedHost": "localhost"}` |
| `HelpRunnerTest.java` / `UiTest.java` + runner test configs | assert/add `kafka` / `kafka-01` where `amqp091-01` appears |
| `sample-plugins` | `KafkaFilter` example (parity with `Amqp091Filter`) |

## 10. Milestones

1. **M1 — Skeleton + passthrough + rewrite** (make a real client work live): module + registration, settings/context/proxy/socket/splitter/translator, GenericRequest/Response with correlation-id matching, ApiVersions capping, Metadata/FindCoordinator/DescribeCluster rewrite, `KafkaImage`.
   *Gate: `SimpleTest` produce/consume through the proxy passes (NullStorageRepository).*
2. **M2 — Codec depth + tagging**: full `KafkaBBuffer`, Produce/Fetch semantic decode, group-id peek decoders.
   *Gate: `CodecTest` green (incl. golden-bytes byte-identical re-encode); recordings carry topic/group/partition tags.*
3. **M3 — Record/replay**: Record + Replay plugins, correlationId preservation, repeatable items, member-id echo.
   *Gate: `ReplayerTest` green broker-less, including consumer-group flow.*
4. **M4 — Full parity**: Publish plugin (real produce, single-record batch encoder w/ CRC32C) + REST APIs + JTE UI, Latency/NetError/Report/RestPlugins, plugin CLIs, `KafkaCommandLineHandler`, `KafkaJteResolver`, runner/jacoco/README/settings integration, `SpecialErrorsTest`, module README.
   *Gate: full build; HelpRunnerTest/UiTest show and drive the kafka instance.*

## 11. Verification

- `mvn -pl protocol-kafka -am test` — codec + container tests (Docker required)
- `mvn install` at root — module registration in both profiles, runner packaging
- End-to-end: run `protocol-runner` with a `kafka` instance; `kafka-clients` producer/consumer through it against the container; record then replay from the UI; publish a message from the UI panel
- `HelpRunnerTest` / `UiTest` confirm CLI and UI registration

## 12. Risks

1. **Pipelining vs blocking `sendAndExpect`**: Kafka clients allow multiple in-flight requests per connection; the framework serializes per connection. Responses are in-order per connection and correlation-id `canRun` matching absorbs bursts — residual risk is throughput only. Contingency (not baseline): `Tagged(Tag.ofKeys("CORRELATION"))` per-request stacks.
2. **`BaseProxySocket.read` 2 s hard deadline vs Fetch long-poll**: safe at the default `fetch.max.wait.ms=500`; document the limit and extend the read deadline in `KafkaProxySocket` for larger waits.
3. **Flexible-encoding decode bugs**: bounded by version capping + golden-bytes byte-identical re-encode tests; tagged fields always opaque.
4. **Idempotent producer**: `InitProducerId` (22) is passthrough; replay returns recorded producerId/epoch deterministically. If sequence/epoch mismatches surface in replay, document `enable.idempotence=false` as the workaround.
5. **Consumer-group rebalance nondeterminism in replay**: member ids/generations differ per run — group APIs match on api+group tags, marked repeatable, member id echoed from the live request.
6. **Record-batch encoding in the publish plugin**: the one place we encode batches (single-record, CRC32C, no compression) — keep it minimal and unit-test against a broker round-trip.
7. **ApiVersions header-v0 exception + in-flight map hygiene**: clean entries on response, bound map size; explicit unit tests.
