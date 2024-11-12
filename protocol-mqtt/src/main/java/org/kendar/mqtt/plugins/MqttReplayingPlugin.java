package org.kendar.mqtt.plugins;

import org.apache.commons.beanutils.BeanUtils;
import org.kendar.filters.BasicReplayingPlugin;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.fsm.ConnectAck;
import org.kendar.mqtt.fsm.Publish;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.FilterContext;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class MqttReplayingPlugin extends BasicReplayingPlugin {
    private static final Logger log = LoggerFactory.getLogger(MqttReplayingPlugin.class);
    private static JsonMapper mapper = new JsonMapper();

    @Override
    public String getProtocol() {
        return "mqtt";
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void buildState(FilterContext filterContext, ProtoContext context, Object in, Object outObj, Object toread) {
        var out = mapper.toJsonNode(outObj);

        var result = mapper.deserialize(out.get("data").toString(), toread.getClass());
        try {
            BeanUtils.copyProperties(toread, result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> storageItems) {
        if (storageItems.isEmpty()) return;
        for (var item : storageItems) {
            var out = mapper.toJsonNode(item.getOutput());
            var clazz = out.get("type").textValue();
            ReturnMessage fr;
            int consumeId = item.getConnectionId();
            switch (clazz) {
                case "ConnectAck":
                    fr = mapper.deserialize(out.get("data").toString(), ConnectAck.class);
                    break;
                case "Publish":
                    fr = mapper.deserialize(out.get("data").toString(), Publish.class);
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
