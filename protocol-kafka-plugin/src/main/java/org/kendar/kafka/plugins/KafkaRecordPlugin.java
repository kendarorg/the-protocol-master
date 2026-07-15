package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.enums.KafkaApiKeys;
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

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var data = new HashMap<String, String>();
        data.put("input", item.getInput() != null
                ? (item.getInputType().equalsIgnoreCase("BBuffer") ? "byte[]" : item.getInputType()) : null);
        data.put("output", item.getOutput() != null ? item.getOutputType() : null);
        var apiKey = apiKeyOf(item);
        if (apiKey != null) {
            data.put("api", KafkaApiKeys.nameOf(apiKey));
        }
        return data;
    }

    /** Parses the api_key (bytes 4-5, after the 4-byte size) from a recorded request frame. */
    private Short apiKeyOf(StorageItem item) {
        try {
            if (item.getInput() == null) {
                return null;
            }
            var node = mapper.toJsonNode(item.getInput());
            if (node == null || node.get("raw") == null) {
                return null;
            }
            var raw = Base64.getDecoder().decode(node.get("raw").asText());
            if (raw.length < 6) {
                return null;
            }
            return (short) (((raw[4] & 0xFF) << 8) | (raw[5] & 0xFF));
        } catch (Exception e) {
            return null;
        }
    }
}
