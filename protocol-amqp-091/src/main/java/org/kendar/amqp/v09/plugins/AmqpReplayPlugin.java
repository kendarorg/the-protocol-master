package org.kendar.amqp.v09.plugins;

import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicCancel;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.PluginException;
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

@TpmService(tags = "amqp091")
public class AmqpReplayPlugin extends BasicReplayPlugin<BasicAysncReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(AmqpReplayPlugin.class);
    private static final List<String> repeatableItems = Arrays.asList(
            "ExchangeDeclare", "QueueDeclare", "QueueBind", "ExchangeBind",
            "BasicConsume", "byte[]", "ConnectionStartOk", "ConnectionOpen",
            "ChannelOpen"
    );


    public AmqpReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

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
                ReturnMessage fr = null;
                switch (clazz) {
                    case "BasicDeliver":
                        var bd = mapper.deserialize(out, BasicDeliver.class);
                        var tag = (String) ctx.getValue("BASIC_CONSUME_CT_" + bd.getConsumeOrigin());

                        log.trace("BasicDeliver tag{}", tag);
                        if (tag != null && !tag.isEmpty()) {
                            bd.setConsumerTag(tag);
                        }
                        fr = bd;
                        break;
                    case "HeaderFrame":
                        fr = mapper.deserialize(out, HeaderFrame.class);
                        break;
                    case "BodyFrame":
                        fr = mapper.deserialize(out, BodyFrame.class);
                        break;
                    case "BasicCancel":
                        fr = mapper.deserialize(out, BasicCancel.class);
                        break;
                }
                if (fr != null) {
                    log.debug("[SERVER][CB]: {}", fr.getClass().getSimpleName());
                    ctx.write(fr);
                } else {
                    throw new PluginException("MISSING CLASS " + clazz);
                }
            }
        }
    }

    protected Map<String, String> getContextTags(ProtoContext context) {
        if (context.getValue("QUEUE") != null) {
            var hashTopic = (HashSet<String>) context.getValue("QUEUE");
            var result = new HashMap<String, String>();
            for (var topic : hashTopic) {
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
        return data;
    }

    @Override
    protected List<String> repeatableItems() {
        return repeatableItems;
    }


}
