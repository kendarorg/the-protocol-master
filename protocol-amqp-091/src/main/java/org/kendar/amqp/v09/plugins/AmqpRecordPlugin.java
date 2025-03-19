package org.kendar.amqp.v09.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TpmService(tags = "amqp091")
public class AmqpRecordPlugin extends BasicRecordPlugin<BasicAysncRecordPluginSettings> {
    private static final List<String> toAvoid = List.of("byte[]",
            "ConnectionStartOk", "ConnectionTuneOk", "ConnectionOpen", "ChannelOpen", "BasicPublish",
            "HeaderFrame", "BasicPublish", "BodyFrame", "BasicAck", "ChannelClose", "ConnectionClose",
            "QueueDeclare", "ExchangeDeclare", "QueueDelete", "ExchangeDelete");

    public AmqpRecordPlugin(JsonMapper mapper, StorageRepository storage,
                            MultiTemplateEngine resolversFactory, SimpleParser parser) {
        super(mapper, storage,resolversFactory,parser);
    }

    private static int getConsumeId(JsonNode data, int consumeId) {
        if (data == null) return consumeId;
        var cid = data.get("consumeId");
        if (cid == null) return consumeId;
        return Math.max(cid.asInt(), consumeId);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicAysncRecordPluginSettings.class;
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

        if (item.getInputType() != null && item.getInputType().equalsIgnoreCase("basicConsume")) {
            var bs = mapper.deserialize(item.getInput(), BasicConsume.class);
            var queue = bs.getQueue() + "|" + bs.getChannel() + "|" + mapper.serialize(bs.getArguments());
            data.put("queue", queue);
        } else if (item.getOutputType() != null && item.getOutputType().equalsIgnoreCase("bodyFrame")) {
            var bs = mapper.deserialize(item.getOutput(), BodyFrame.class);
            data.put("queue", bs.getConsumeOrigin());
        } else if (item.getOutputType() != null && item.getOutputType().equalsIgnoreCase("basicDeliver")) {
            var bs = mapper.deserialize(item.getOutput(), BasicDeliver.class);
            data.put("queue", bs.getConsumeOrigin());
        } else if (item.getOutputType() != null && item.getOutputType().equalsIgnoreCase("headerFrame")) {
            var bs = mapper.deserialize(item.getOutput(), HeaderFrame.class);
            data.put("queue", bs.getConsumeOrigin());
        }
        var consumeId = 0;
        data.put("input", null);
        data.put("output", null);
        data.put("consumeId", null);
        if (item.getInput() != null) {
            data.put("input", item.getInputType().equalsIgnoreCase("BBuffer") ?
                    "byte[]" : item.getInputType());
            if (item.getInputType() != null) {
                consumeId = getConsumeId(out, consumeId);
            }
        }
        if (item.getOutput() != null) {
            data.put("output", item.getOutputType().equalsIgnoreCase("BBuffer") ?
                    "byte[]" : item.getOutputType());
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
