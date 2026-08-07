package org.kendar.kafka.plugins;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.utils.KafkaFrameDescriber;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.pf4j.Extension;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records Kafka traffic. The proxy relays raw frames ({@code KafkaRawMessage}
 * request / {@code *Response} reply); each interaction is tagged with the API
 * name (parsed from the request header) so recordings stay browsable. Everything
 * is saved (including ApiVersions/Metadata) so broker-less replay can answer the
 * client handshake.
 */
@Extension
@TpmService(tags = "kafka")
public class KafkaRecordPlugin extends BasicRecordPlugin<BasicAysncRecordPluginSettings> {

    public KafkaRecordPlugin(JsonMapper mapper, StorageRepository storage,
                             MultiTemplateEngine resolversFactory, SimpleParser parser) {
        super(mapper, storage, resolversFactory, parser);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicAysncRecordPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }

    @Override
    protected boolean shouldNotSave(Object in, Object out, CompactLine cl) {
        return false;
    }

    /**
     * Builds the index tags AND enriches the item with a readable {@code decoded}
     * view next to the raw bytes ({@link KafkaFrameDescriber}) — this is the only
     * hook that sees the serialized request and response together before the item
     * is persisted. Replay keeps reading only the top-level {@code raw}/{@code payload}.
     */
    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var data = new HashMap<String, String>();
        data.put("input", item.getInput() != null
                ? (item.getInputType().equalsIgnoreCase("BBuffer") ? "byte[]" : item.getInputType()) : null);
        data.put("output", item.getOutput() != null ? item.getOutputType() : null);
        var reqRaw = rawOf(item.getInput(), "raw");
        if (reqRaw == null || reqRaw.length < 8) {
            return data;
        }
        short apiKey = (short) (((reqRaw[4] & 0xFF) << 8) | (reqRaw[5] & 0xFF));
        short apiVersion = (short) (((reqRaw[6] & 0xFF) << 8) | (reqRaw[7] & 0xFF));
        data.put("api", KafkaApiKeys.nameOf(apiKey));

        var decodedReq = KafkaFrameDescriber.describeRequest(reqRaw);
        attach(item.getInput(), decodedReq);
        var topic = firstTopicOf(decodedReq);
        if (topic != null) {
            data.put("topic", topic);
        }
        var resRaw = rawOf(item.getOutput(), "payload");
        if (resRaw != null) {
            attach(item.getOutput(), KafkaFrameDescriber.describeResponse(resRaw, apiKey, apiVersion));
        }
        return data;
    }

    /** Base-64 frame bytes from the serialized message ({@code raw} / {@code payload}). */
    private byte[] rawOf(Object serialized, String field) {
        try {
            if (serialized == null) {
                return null;
            }
            var node = mapper.toJsonNode(serialized);
            if (node == null || node.get(field) == null || node.get(field).isNull()) {
                return null;
            }
            return Base64.getDecoder().decode(node.get(field).asText());
        } catch (Exception e) {
            return null;
        }
    }

    private void attach(Object serialized, Map<String, Object> decoded) {
        if (serialized instanceof ObjectNode) {
            ((ObjectNode) serialized).set("decoded", mapper.toJsonNode(decoded));
        }
    }

    @SuppressWarnings("unchecked")
    private String firstTopicOf(Map<String, Object> decoded) {
        var topics = decoded.get("topics");
        if (topics instanceof List && !((List<?>) topics).isEmpty()) {
            var first = ((List<?>) topics).get(0);
            if (first instanceof Map) {
                var name = ((Map<String, Object>) first).get("topic");
                return name != null ? name.toString() : null;
            }
        }
        return null;
    }
}
