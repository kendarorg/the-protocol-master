package org.kendar.mqtt.plugins;

import org.apache.commons.beanutils.BeanUtils;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.PluginException;
import org.kendar.mqtt.fsm.ConnectAck;
import org.kendar.mqtt.fsm.Publish;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.ExtraBeanUtils;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@TpmService(tags = "mqtt")
public class MqttReplayPlugin extends BasicReplayPlugin<BasicAysncReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(MqttReplayPlugin.class);
    private static final List<String> repeatableItems = Arrays.asList(
            "Connect", "Subscribe", "Publish"
    );


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
            throw new PluginException(e);
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
                ReturnMessage fr = switch (clazz) {
                    case "ConnectAck" -> mapper.deserialize(out.toString(), ConnectAck.class);
                    case "Publish" -> mapper.deserialize(out.toString(), Publish.class);
                    default -> throw new PluginException("MISSING " + clazz);
                };
                if (fr != null) {
                    log.debug("[SERVER][CB]: {}", fr.getClass().getSimpleName());
                    ctx.write(fr);
                } else {
                    throw new PluginException("MISSING CLASS " + clazz);
                }
            }
        }
    }

    @SuppressWarnings({"RegExpEmptyAlternationBranch", "SuspiciousRegexArgument"})
    @Override
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

    @Override
    protected List<String> repeatableItems() {
        return repeatableItems;
    }

}
