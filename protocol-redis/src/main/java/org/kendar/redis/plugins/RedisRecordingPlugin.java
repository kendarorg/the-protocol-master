package org.kendar.redis.plugins;

import org.kendar.filters.BasicRecordingPlugin;
import org.kendar.proxy.FilterContext;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

public class RedisRecordingPlugin extends BasicRecordingPlugin
{
    @Override
    protected Object getData(Object of) {
        if (of instanceof Resp3Message) {
            return ((Resp3Message) of).getData();
        }
        if (of instanceof Resp3Response) {
            return getData(((Resp3Response) of).getEvent());
        }
        return of;
    }

    @Override
    public String getProtocol() {
        return "redis";
    }

    protected void asyncCall(FilterContext filterContext, Object out) {
        var duration = 0;

        var res = "{\"type\":\"RESPONSE\",\"data\":" + mapper.serialize(out) + "}";
        var req = "{\"type\":null,\"data\":null}";

        var storageItem = new StorageItem(filterContext.getContext().getContextId(),
                mapper.toJsonNode(req),
                mapper.toJsonNode(res),
                duration, filterContext.getType(),
                filterContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
    }
}
