package org.kendar.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mqtt.utils.MqttStorage;
import org.kendar.storage.BaseStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MqttStorageHandler extends BaseStorage<JsonNode, JsonNode> implements MqttStorage {
    private static final List<String> toAvoid = List.of("Disconnect", "PingReq");

    public MqttStorageHandler(StorageRepository<JsonNode, JsonNode> repository) {
        super(repository);
    }


    private static int getConsumeId(JsonNode output, int consumeId) {
        if (output == null) return 0;
        var data = output.get("data");
        if (data == null) return consumeId;
        var cid = data.get("consumeId");
        if (cid == null) return consumeId;
        return Math.max(cid.asInt(), consumeId);
    }

    @Override
    public String getCaller() {
        return "MQTT";
    }

    @Override
    public TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JsonNode, JsonNode>>() {
        };
    }

    @Override
    public boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JsonNode, JsonNode> item, List<StorageItem<JsonNode, JsonNode>> loadedData) {
        if (useFullData) return false;
        if (cl == null) return false;
        if (cl.getTags() == null || cl.getTags().get("input") == null) {
            return false;
        }
        return toAvoid.contains(cl.getTags().get("input"));
    }

    @Override
    public Map<String, String> buildTag(StorageItem<JsonNode, JsonNode> item) {
        var data = new HashMap<String, String>();
        var consumeId = 0;
        data.put("input", null);
        data.put("output", null);
        data.put("consumeId", null);
        if (item.getInput() != null) {
            if (item.getInput().get("type") != null) {
                data.put("input", item.getInput().get("type").textValue());
                consumeId = getConsumeId(item.getOutput(), consumeId);
            }
        }
        if (item.getOutput() != null) {
            if (item.getOutput().get("type") != null) {
                data.put("output", item.getOutput().get("type").textValue());
                consumeId = getConsumeId(item.getOutput(), consumeId);
            }
        }
        if (consumeId > 0) {
            data.put("consumeId", consumeId + "");
        }
        return data;
    }

}
