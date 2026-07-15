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
     * Patch the live request's correlation id into the recorded response so the
     * client accepts it. {@code in} is the live {@link KafkaRawMessage} request;
     * {@code toRead} is the {@link KafkaResponseState} about to be written back.
     */
    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in,
                              Object outObj, Object toRead, LineToRead lineToRead) {
        if (!(in instanceof KafkaRawMessage) || !(toRead instanceof KafkaResponseState)) {
            return;
        }
        var reqRaw = ((KafkaRawMessage) in).getRaw();
        var resp = (KafkaResponseState) toRead;
        var payload = resp.getPayload();
        if (reqRaw == null || reqRaw.length < 8 || payload == null || payload.length < 8) {
            return;
        }
        // correlation id = bytes 4..7 (big-endian) after the 4-byte size.
        System.arraycopy(reqRaw, 4, payload, 4, 4);
        resp.setPayload(payload);
        log.debug("[REPLAY] patched recorded response with live correlation id");
    }
}
