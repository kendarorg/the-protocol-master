package org.kendar.redis.plugins;

import org.kendar.filters.BasicReplayingPlugin;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.FilterContext;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;

import java.util.List;

public class RedisReplayingPlugin extends BasicReplayingPlugin {
    @Override
    public String getProtocol() {
        return "redis";
    }

    private static JsonMapper mapper = new JsonMapper();

    @Override
    protected void buildState(FilterContext filterContext, ProtoContext context, Object in, Object outObj, Object toread) {
        var out = mapper.toJsonNode(outObj);
        ((Resp3Response)toread).execute(new Resp3Message(context, null, out.get("data")));
    }

    @Override
    protected void sendBackResponses(ProtoContext context,List<StorageItem> storageItems) {
        if (storageItems.isEmpty()) return;
        for (var item : storageItems) {
            var out = mapper.toJsonNode(item.getOutput());
            var type = out.get("type").textValue();

            int connectionId = item.getConnectionId();
            if (type.equalsIgnoreCase("RESPONSE")) {
                var ctx = ((Resp3Protocol)context.getDescriptor()).consumeContext.get(connectionId);
                ReturnMessage fr = new Resp3Message(ctx, null, out.get("data"));
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING RESPONSE_CLASS");
            }

        }
    }
}
