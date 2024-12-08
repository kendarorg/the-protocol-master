package org.kendar.mqtt.plugins;

import org.apache.commons.beanutils.BeanUtils;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.fsm.ConnectAck;
import org.kendar.mqtt.fsm.Publish;
import org.kendar.plugins.ReplayPlugin;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToRead;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class MqttReplayPlugin extends ReplayPlugin<BasicReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(MqttReplayPlugin.class);

    @Override
    public String getProtocol() {
        return "mqtt";
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outObj, Object toread, LineToRead lineToRead) {
        var out = mapper.toJsonNode(outObj);

        var result = mapper.deserialize(out.toString(), toread.getClass());
        try {
            BeanUtils.copyProperties(toread, result);
        } catch (IllegalAccessException | InvocationTargetException e) {
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
            ReturnMessage fr;
            int consumeId = item.getConnectionId();
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
                var ctx = MqttProtocol.consumeContext.get(consumeId);
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING CLASS " + clazz);
            }

        }
    }
}
