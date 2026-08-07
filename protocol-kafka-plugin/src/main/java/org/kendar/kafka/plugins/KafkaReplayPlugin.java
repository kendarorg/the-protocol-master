package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.fsm.KafkaResponseState;
import org.kendar.kafka.messages.KafkaRawMessage;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.ReplayFindIndexResult;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.CompactLine;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Broker-less replay of recorded Kafka sessions. Kafka is a pure request/response
 * protocol ({@code hasCallbacks() == false}): on each client request the base
 * plugin matches the recorded interaction (by api + stable request content) and
 * returns the recorded response.
 * <p>
 * The recorded response carries the <i>recorded</i> correlation id; before it is
 * relayed we patch in the <b>live</b> request's correlation id (bytes 4-7 of the
 * frame, right after the size prefix) so the client's in-flight tracking matches
 * — the analog of MQTT preserving the PacketIdentifier (protocol-kafka.md §7).
 */
@Extension
@TpmService(tags = "kafka")
public class KafkaReplayPlugin extends BasicReplayPlugin<BasicAysncReplayPluginSettings> {
    private static final Logger log = LoggerFactory.getLogger(KafkaReplayPlugin.class);
    /**
     * APIs a client re-issues in its poll/handshake loops (metadata refresh, fetch
     * polling, group heartbeats). Once every recorded instance is consumed, further
     * calls replay the best already-used one instead of failing the match — Produce
     * and topic-mutating APIs stay strictly one-shot.
     */
    private static final Set<String> REPEATABLE_APIS = Set.of(
            "ApiVersions", "Metadata", "FindCoordinator", "DescribeCluster",
            "ListOffsets", "Fetch", "Heartbeat", "OffsetFetch");

    public KafkaReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicAysncReplayPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }

    @Override
    protected boolean hasCallbacks() {
        return false;
    }

    /**
     * Strict per-API matching. The generic matcher falls back to "best unused index
     * even with zero tag matches", which lets a timing-dependent extra request (e.g.
     * a metadata refresh) steal a recorded response of a DIFFERENT api — the client
     * then misdecodes it. Here a request only ever matches an index with the same
     * {@code api} tag: earliest unused wins; once exhausted, poll-loop APIs replay
     * the last recorded instance again, anything else is a genuine miss.
     */
    @Override
    protected ReplayFindIndexResult findIndex(CallItemsQuery query, Object in) {
        var api = query.getTags().get("api");
        var candidates = getIndexes().stream()
                .filter(a -> !"RESPONSE".equalsIgnoreCase(a.getType()))
                .filter(a -> a.getCaller().equalsIgnoreCase(query.getCaller()))
                .filter(a -> Objects.equals(api, a.getTags().get("api")))
                .sorted(Comparator.comparingLong(CompactLine::getIndex))
                .toList();
        for (var candidate : candidates) {
            if (query.getUsed().stream().noneMatch(n -> n == candidate.getIndex())) {
                log.debug("[REPLAY] matched api {} at index {}", api, candidate.getIndex());
                return new ReplayFindIndexResult(candidate, false);
            }
        }
        if (REPEATABLE_APIS.contains(api) && !candidates.isEmpty()) {
            var last = candidates.get(candidates.size() - 1);
            log.debug("[REPLAY] repeating api {} from index {}", api, last.getIndex());
            return new ReplayFindIndexResult(mapper.clone(last), true);
        }
        log.error("[REPLAY] no recorded response for api {} ({})", api, query);
        return null;
    }

    @Override
    protected Map<String, String> buildTag(Object in) {
        var data = new HashMap<String, String>();
        if (in instanceof KafkaRawMessage) {
            var raw = ((KafkaRawMessage) in).getRaw();
            if (raw != null && raw.length >= 6) {
                short apiKey = (short) (((raw[4] & 0xFF) << 8) | (raw[5] & 0xFF));
                data.put("api", KafkaApiKeys.nameOf(apiKey));
            }
        }
        return data;
    }

    /**
     * Load the recorded response bytes into the (empty) response state and patch in
     * the live request's correlation id, so the client accepts the broker-less reply.
     * {@code in} is the live {@link KafkaRawMessage} request; {@code outObj} is the
     * recorded output ({@link KafkaResponseState} JSON with a base-64 {@code payload});
     * {@code toRead} is the {@link KafkaResponseState} the request state will return.
     */
    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in,
                              Object outObj, Object toRead, LineToRead lineToRead) {
        if (!(in instanceof KafkaRawMessage) || !(toRead instanceof KafkaResponseState) || outObj == null) {
            return;
        }
        var reqRaw = ((KafkaRawMessage) in).getRaw();
        var payload = recordedPayload(outObj);
        if (reqRaw == null || reqRaw.length < 12 || payload == null || payload.length < 8) {
            return;
        }
        // request corr id is at offset 8 (size[4] + apiKey[2] + apiVersion[2]);
        // response corr id is at offset 4 (size[4]). Copy live req corr -> recorded resp.
        System.arraycopy(reqRaw, 8, payload, 4, 4);
        ((KafkaResponseState) toRead).setPayload(payload);
        log.debug("[REPLAY] loaded recorded response ({} bytes) with live correlation id", payload.length);
    }

    /** Extracts the base-64 {@code payload} from a recorded response (any *Response type). */
    private byte[] recordedPayload(Object outObj) {
        try {
            var node = mapper.toJsonNode(outObj);
            if (node == null || node.get("payload") == null) {
                return null;
            }
            return Base64.getDecoder().decode(node.get("payload").asText());
        } catch (Exception e) {
            log.error("[REPLAY] cannot decode recorded payload", e);
            return null;
        }
    }
}
