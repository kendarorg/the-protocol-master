package org.kendar.amqp.v09.plugins;

import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicCancel;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.plugins.ReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToRead;
import org.kendar.utils.ExtraBeanUtils;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AmqpReplayPlugin extends ReplayPlugin<BasicAysncReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(AmqpReplayPlugin.class);


    @Override
    public Class<?> getSettingClass() {
        return BasicAysncReplayPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outObj, Object toread, LineToRead lineToRead) {
        if (outObj == null) return;
        if (toread == null) return;
        var out = mapper.toJsonNode(outObj);

        var result = mapper.deserialize(out, toread.getClass());
        try {
            ExtraBeanUtils.copyProperties(toread, result, "Reserved1", "consumerTag");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> storageItems) {
        if (storageItems.isEmpty()) return;
        long lastTimestamp = 0;
        for (var item : storageItems) {
            if (getSettings().isRespectCallDuration()) {
                if (lastTimestamp == 0) {
                    lastTimestamp = item.getTimestamp();
                } else if (item.getTimestamp() > 0) {
                    var wait = item.getTimestamp() - lastTimestamp;
                    lastTimestamp = item.getTimestamp();
                    if (wait > 0) {
                        Sleeper.sleep(wait);
                    }
                }
            }
            var out = mapper.toJsonNode(item.getOutput());
            var clazz = item.getOutputType();
            ReturnMessage fr = null;
            int consumeId = item.getConnectionId();
            switch (clazz) {
                case "BasicDeliver":
                    var bd = mapper.deserialize(out, BasicDeliver.class);
                    var tag = (String) context.getValue("BASIC_CONSUME_CT_" + bd.getConsumeOrigin());
                    bd.setConsumerTag(tag);
                    //consumeId = bd.getConsumeId();
                    fr = bd;
                    break;
                case "HeaderFrame":
                    var hf = mapper.deserialize(out, HeaderFrame.class);
                    //consumeId = hf.getConsumeId();
                    fr = hf;
                    break;
                case "BodyFrame":
                    var bf = mapper.deserialize(out, BodyFrame.class);
                    //consumeId = bf.getConsumeId();
                    fr = bf;
                    break;
                case "BasicCancel":
                    var bc = mapper.deserialize(out, BasicCancel.class);
                    //consumeId = bc.getConsumeId();
                    fr = bc;
                    break;
            }
            if (fr != null) {
                log.debug("[SERVER][CB]: {}", fr.getClass().getSimpleName());
                var ctx = ((AmqpProtocol) context.getDescriptor()).getConsumeContext().get(consumeId);
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING CLASS " + clazz);
            }
        }
    }

    protected Map<String, String> getContextTags(ProtoContext context) {
        if (context.getValue("QUEUE") != null) {
            var hashTopic = (HashSet<String>) context.getValue("QUEUE");
            var result = new HashMap<String, String>();
            for (var topic : hashTopic) {
                // var spl = topic.split("|",3);
                //TODO RABBITCONTEXT
                result.put("queue", topic);
            }
            return result;
        }
        return Map.of();
    }

    @Override
    protected Map<String, String> buildTag(Object cll) {
        var data = new HashMap<String, String>();
        if (cll instanceof BasicConsume) {
            data.put("queue", ((BasicConsume) cll).getConsumeOrigin());
        } else if (cll instanceof BodyFrame) {
            data.put("queue", ((BodyFrame) cll).getConsumeOrigin());
        } else if (cll instanceof BasicDeliver) {
            data.put("queue", ((BasicDeliver) cll).getConsumeOrigin());
        } else if (cll instanceof HeaderFrame) {
            data.put("queue", ((HeaderFrame) cll).getConsumeOrigin());
        }
        //var in = mapper.toJsonNode(cll);
//        if(in.has("packetIdentifier")){
//            //data.put("packetIdentifier", in.get("packetIdentifier").asText());
//        }
//        if(in.has("topicName")){
//            data.put(in.get("topicName").asText(), in.get("qos").asText());
//        }else if(in.has("topics")){
//            for(var topic : in.get("topics")){
//                data.put(topic.get("topic").asText(), topic.get("type").asText());
//            }
//        }
        return data;
    }
}
