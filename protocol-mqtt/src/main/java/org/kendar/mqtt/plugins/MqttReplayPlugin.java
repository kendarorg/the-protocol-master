package org.kendar.mqtt.plugins;

import org.apache.commons.beanutils.BeanUtils;
import org.kendar.di.annotations.TpmService;
import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.fsm.*;
import org.kendar.plugins.ReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ExtraBeanUtils;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@TpmService(tags = "mqtt")
public class MqttReplayPlugin extends ReplayPlugin<BasicAysncReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(MqttReplayPlugin.class);

    public MqttReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }


    @Override
    public Class<?> getSettingClass() {
        return BasicAysncReplayPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context,
                              Object in, Object outObj, Object toread,
                              LineToRead lineToRead) {
        var out = mapper.toJsonNode(outObj);

        var result = mapper.deserialize(out.toString(), toread.getClass());
        try {
            ExtraBeanUtils.copyProperties(toread, result, "PacketIdentifier");
            BeanUtils.copyProperties(toread, result);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> storageItems) {
        if (storageItems.isEmpty()) return;
        long lastTimestamp = 0;
        for (var item : storageItems) {
            int consumeId = item.getConnectionId();
            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", consumeId + "")) {
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
                var ctx = context.getDescriptor().getContextsCache().get(consumeId);
                var out = mapper.toJsonNode(item.getOutput());
                var clazz = item.getOutputType();
                ReturnMessage fr;
                switch (clazz) {
                    case "ConnectAck":
                        fr = mapper.deserialize(out.toString(), ConnectAck.class);
                        break;
                    case "Publish":
                        fr = mapper.deserialize(out.toString(), Publish.class);
                        break;
                    default:
                        throw new RuntimeException("MISSING " + clazz);

                }
                if (fr != null) {
                    log.debug("[SERVER][CB]: {}", fr.getClass().getSimpleName());
                    ctx.write(fr);
                } else {
                    throw new RuntimeException("MISSING CLASS " + clazz);
                }
            }
        }
    }

    @Override
    protected CompactLine findIndex(CallItemsQuery query, Object in) {
        var result = super.findIndex(query, in);
        var index = -1L;
        if (result != null) {
            index = result.getIndex();
        }
        switch (query.getType()) {
            case "Connect": {
                var cl = new CompactLine();
                cl.setType("Connect");
                cl.setCaller("MQTT");
                cl.setIndex(index);
                cl.getTags().put("input", "Connect");
                cl.getTags().put("output", "ConnectAck");
                return cl;
            }
            case "Subscribe": {
                var cl = new CompactLine();
                cl.setType("Subscribe");
                cl.setCaller("MQTT");
                cl.setIndex(index);
                cl.getTags().put("input", "Subscribe");
                cl.getTags().put("output", "SubscribeAck");
                return cl;
            }
            case "Publish": {
                var cl = new CompactLine();
                cl.setType("Publish");
                cl.setCaller("MQTT");
                cl.setIndex(index);
                var pub = (Publish) in;
                cl.getTags().put("input", "Publish");
                if (pub.getQos() == 1) {
                    cl.getTags().put("output", "PublishAck");
                } else if (pub.getQos() == 2) {
                    cl.getTags().put("output", "PublishRec");
                }
                return cl;
            }
        }
        return result;
    }

    @Override
    protected StorageItem readStorageItem(CompactLine index, Object in, PluginContext pluginContext) {
        var resultFinal = super.readStorageItem(index, in, pluginContext);
        StorageItem result = null;
        //if(result==null && index.getIndex()==-1){
        switch (index.getType()) {
            case "Connect":
                result = handleFakeConnect((Connect) in, pluginContext);
                if (result == null && !getSettings().isBlockExternal()) {
                    index.setIndex(-1);
                    return null;
                }
                break;
            case "Subscribe":
                result = handleFakeSubscribe((Subscribe) in, pluginContext);
                if (result == null && !getSettings().isBlockExternal()) {
                    index.setIndex(-1);
                    return null;
                }
                break;
            case "Publish":
                result = handleFakePublish((Publish) in, pluginContext);
                if (result == null && !getSettings().isBlockExternal()) {
                    index.setIndex(-1);
                    return null;
                }
                break;
            case "PublishRel":
                result = handleFakePublishRel((PublishRel) in, pluginContext);
                if (result == null && !getSettings().isBlockExternal()) {
                    index.setIndex(-1);
                    return null;
                }
                break;
        }
        if (result != null && getSettings().isBlockExternal()) {
            if (resultFinal == null) return result;
        }
        return resultFinal;
    }

    private StorageItem handleFakePublishRel(PublishRel in, PluginContext pluginContext) {
        StorageItem result;
        //Connect for real
        if (!getSettings().isBlockExternal()) {
            return null;
        }
        var context = (MqttContext) pluginContext.getContext();
        var publish = in;

        var subscribeAck = new PublishComp();
        subscribeAck.setFullFlag((byte) 112);
        subscribeAck.setPacketIdentifier(in.getPacketIdentifier());
        subscribeAck.setProtocolVersion(context.getProtocolVersion());
        subscribeAck.setProtoDescriptor(context.getDescriptor());
        result = new StorageItem();
        result.setType("PublishRel");
        result.setInputType("PublishRel");
        result.setOutputType("PublishComp");
        result.setConnectionId(pluginContext.getContextId());
        result.setInput(publish);
        result.setOutput(subscribeAck);
        return result;
    }


    protected Map<String, String> getContextTags(ProtoContext context) {
        if (context.getValue("TOPICS") != null) {
            var hashTopic = (HashSet<String>) context.getValue("TOPICS");
            var result = new HashMap<String, String>();
            for (var topic : hashTopic) {
                var spl = topic.split("|", 2);
                result.put(spl[1].substring(1), spl[0]);
            }
            return result;
        }
        return Map.of();
    }

    @Override
    protected Map<String, String> buildTag(Object cll) {
        var data = new HashMap<String, String>();
        var in = mapper.toJsonNode(cll);
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
        return data;
    }

    private StorageItem handleFakePublish(Publish in, PluginContext pluginContext) {
        StorageItem result;
        //Connect for real
        if (!getSettings().isBlockExternal()) {
            return null;
        }
        var context = (MqttContext) pluginContext.getContext();
        var publish = in;
        if (publish.getQos() == 0) {
            return null;
        } else if (publish.getQos() == 1) {
            var subscribeAck = new PublishAck();
            subscribeAck.setFullFlag((byte) 64);
            subscribeAck.setPacketIdentifier(in.getPacketIdentifier());
            subscribeAck.setProtocolVersion(context.getProtocolVersion());
            subscribeAck.setProtoDescriptor(context.getDescriptor());
            result = new StorageItem();
            result.setType("Publish");
            result.setInputType("Publish");
            result.setOutputType("PublishAck");
            result.setConnectionId(pluginContext.getContextId());
            result.setInput(publish);
            result.setOutput(subscribeAck);
            return result;
        } else if (publish.getQos() == 2) {
            var subscribeAck = new PublishRec();
            subscribeAck.setFullFlag((byte) 80);
            subscribeAck.setReasonCode((byte) 0);
            subscribeAck.setPacketIdentifier(in.getPacketIdentifier());
            subscribeAck.setProtocolVersion(context.getProtocolVersion());
            subscribeAck.setProtoDescriptor(context.getDescriptor());
            result = new StorageItem();
            result.setType("Publish");
            result.setInputType("Publish");
            result.setOutputType("PublishRec");
            result.setConnectionId(pluginContext.getContextId());
            result.setInput(publish);
            result.setOutput(subscribeAck);
            return result;
        }
        return null;
    }

    private StorageItem handleFakeSubscribe(Subscribe in, PluginContext pluginContext) {
        StorageItem result;
        //Connect for real
        if (!getSettings().isBlockExternal()) {
            return null;
        }
        var context = (MqttContext) pluginContext.getContext();
        var subscribe = in;
        var subscribeAck = new SubscribeAck();
        subscribeAck.setFullFlag((byte) -112);
        subscribeAck.setPacketIdentifier(in.getPacketIdentifier());
        subscribeAck.setProtocolVersion(context.getProtocolVersion());
        subscribeAck.setProtoDescriptor(context.getDescriptor());
        result = new StorageItem();
        result.setType("Subscribe");
        result.setInputType("Subscribe");
        result.setOutputType("SubscribeAck");
        result.setConnectionId(pluginContext.getContextId());
        result.setInput(subscribe);
        result.setOutput(subscribeAck);
        return result;
    }

    private StorageItem handleFakeConnect(Connect in, PluginContext pluginContext) {
        StorageItem result;
        //Connect for real
        if (!getSettings().isBlockExternal()) {
            return null;
        }
        var context = (MqttContext) pluginContext.getContext();
        var connect = in;
        var connectAck = new ConnectAck();
        connectAck.setFullFlag((byte) 32);
        connectAck.setSessionSet(false);
        connectAck.setConnectReasonCode((byte) 0);
        connectAck.setProtocolVersion(context.getProtocolVersion());
        connectAck.setProtoDescriptor(context.getDescriptor());
        result = new StorageItem();
        result.setType("Connect");
        result.setInputType("Connect");
        result.setOutputType("ConnectAck");
        result.setConnectionId(pluginContext.getContextId());
        result.setInput(connect);
        result.setOutput(connectAck);
        return result;
    }
}
