package org.kendar.mqtt.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.settings.BasicAsyncRecordPluginSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TpmService(tags = "mqtt")
public class MqttRecordPlugin extends BasicRecordPlugin<BasicAsyncRecordPluginSettings> {
    private static final List<String> toAvoid = List.of("Disconnect", "PingReq");

    public MqttRecordPlugin(JsonMapper mapper, StorageRepository storage,
                            MultiTemplateEngine resolversFactory, SimpleParser parser) {
        super(mapper, storage, resolversFactory, parser);
    }

    private static int getConsumeId(JsonNode data, int consumeId) {
        if (data == null) return consumeId;
        var cid = data.get("consumeId");
        if (cid == null) return consumeId;
        return Math.max(cid.asInt(), consumeId);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicAsyncRecordPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }

    @Override
    protected boolean shouldNotSave(Object in, Object out, CompactLine cl) {
        if (cl == null) return false;
        if (cl.getTags() == null || cl.getTags().get("input") == null) {
            return false;
        }
        return toAvoid.contains(cl.getTags().get("input"));
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        var data = new HashMap<String, String>();
        var in = mapper.toJsonNode(item.getInput());
        var out = mapper.toJsonNode(item.getOutput());
        var consumeId = 0;
        data.put("input", null);
        data.put("output", null);
        data.put("consumeId", null);
        if (item.getInput() != null) {
            if (in.has("packetIdentifier")) {
                //data.put("packetIdentifier", in.get("packetIdentifier").asText());
            }
            if (in.has("topicName")) {
                data.put(in.get("topicName").asText(), in.get("qos").asText());
            } else if (in.has("topics")) {
                for (var topic : in.get("topics")) {
                    data.put(topic.get("topic").asText(), topic.get("type").asText());
                }
            }
            if (item.getInputType() != null) {
                consumeId = getConsumeId(out, consumeId);
            }
        }
        if (item.getOutput() != null) {
            if (out.has("packetIdentifier")) {
                //data.put("packetIdentifier", out.get("packetIdentifier").asText());
            }
            if (out.has("topicName")) {
                data.put(out.get("topicName").asText(), out.get("qos").asText());
            }
            if (item.getOutputType() != null) {
                consumeId = getConsumeId(out, consumeId);
            }
        }
        if (consumeId > 0) {
            data.put("consumeId", consumeId + "");
        }
        return data;
    }
}
