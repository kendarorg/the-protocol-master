package org.kendar.amqp.v09.plugins;

import org.apache.commons.beanutils.BeanUtils;
import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicCancel;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.plugins.ReplayPlugin;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class AmqpReplayPlugin extends ReplayPlugin<BasicReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(AmqpReplayPlugin.class);

    @Override
    public String getProtocol() {
        return "amqp091";
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outObj, Object toread) {
        if (outObj == null) return;
        if (toread == null) return;
        var out = mapper.toJsonNode(outObj);

        var result = mapper.deserialize(out, toread.getClass());
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
            var clazz = item.getOutputType();
            ReturnMessage fr = null;
            int consumeId = -1;
            switch (clazz) {
                case "BasicDeliver":
                    var bd = mapper.deserialize(out, BasicDeliver.class);
                    consumeId = bd.getConsumeId();
                    fr = bd;
                    break;
                case "HeaderFrame":
                    var hf = mapper.deserialize(out, HeaderFrame.class);
                    consumeId = hf.getConsumeId();
                    fr = hf;
                    break;
                case "BodyFrame":
                    var bf = mapper.deserialize(out, BodyFrame.class);
                    consumeId = bf.getConsumeId();
                    fr = bf;
                    break;
                case "BasicCancel":
                    var bc = mapper.deserialize(out, BasicCancel.class);
                    consumeId = bc.getConsumeId();
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
}
