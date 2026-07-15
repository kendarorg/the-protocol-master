package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.fsm.KafkaResponseState;
import org.kendar.kafka.messages.KafkaRawMessage;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
