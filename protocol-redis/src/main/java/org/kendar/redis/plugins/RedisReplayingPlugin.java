package org.kendar.redis.plugins;

import org.kendar.plugins.ReplayingPlugin;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;

import java.util.List;

public class RedisReplayingPlugin extends ReplayingPlugin {
    private static final JsonMapper mapper = new JsonMapper();

    @Override
    public String getProtocol() {
        return "redis";
    }

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outObj, Object toread) {
        var out = mapper.toJsonNode(outObj);
        ((Resp3Response) toread).execute(new Resp3Message(context, null, out));
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> storageItems) {
        if (storageItems.isEmpty()) return;
        for (var item : storageItems) {
            var out = mapper.toJsonNode(item.getOutput());
            var type = item.getOutputType();

            int connectionId = item.getConnectionId();
            if (type.equalsIgnoreCase("RESPONSE")) {
                var ctx = ((Resp3Protocol) context.getDescriptor()).consumeContext.get(connectionId);
                ReturnMessage fr = new Resp3Message(ctx, null, out);
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING RESPONSE_CLASS");
            }

        }
    }
}
