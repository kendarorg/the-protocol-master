package org.kendar.amqp.v09.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.plugins.RecordingPlugin;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmqpRecordingPlugin extends RecordingPlugin {
    private static final List<String> toAvoid = List.of("byte[]",
            "ConnectionStartOk", "ConnectionTuneOk", "ConnectionOpen", "ChannelOpen", "BasicPublish",
            "HeaderFrame", "BasicPublish", "BodyFrame", "BasicAck", "ChannelClose", "ConnectionClose",
            "QueueDeclare", "ExchangeDeclare", "QueueDelete", "ExchangeDelete");

    private static int getConsumeId(JsonNode data, int consumeId) {
        if (data == null) return consumeId;
        var cid = data.get("consumeId");
        if (cid == null) return consumeId;
        return Math.max(cid.asInt(), consumeId);
    }


    @Override
    public String getProtocol() {
        return "amqp091";
    }

    @Override
    protected boolean shouldNotSave(Object in, Object out, CompactLine cl) {
        if (cl == null) return false;
        if (cl.getTags() == null || cl.getTags().get("input") == null) {
            return false;
        }
        return toAvoid.contains(cl.getTags().get("input")) || toAvoid.contains(cl.getTags().get("output"));
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
            data.put("input", item.getInputType().equalsIgnoreCase("BBuffer")?
                        "byte[]":item.getInputType());
            if (item.getInputType() != null) {
                consumeId = getConsumeId(out, consumeId);
            }
        }
        if (item.getOutput() != null) {
            data.put("output", item.getOutputType().equalsIgnoreCase("BBuffer")?
                    "byte[]":item.getOutputType());
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
