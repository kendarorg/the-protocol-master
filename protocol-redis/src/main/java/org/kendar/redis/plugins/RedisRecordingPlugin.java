package org.kendar.redis.plugins;

import org.kendar.plugins.RecordingPlugin;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;

public class RedisRecordingPlugin extends RecordingPlugin {
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

    protected void asyncCall(PluginContext pluginContext, Object out) {
        var duration = 0;

        var res = "{\"type\":\"RESPONSE\",\"data\":" + mapper.serialize(out) + "}";
        var req = "{\"type\":null,\"data\":null}";

        var storageItem = new StorageItem(pluginContext.getContextId(),
                mapper.toJsonNode(req),
                mapper.toJsonNode(res),
                duration, pluginContext.getType(),
                pluginContext.getCaller());
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        storage.write(new LineToWrite(getInstanceId(), storageItem, compactLine));
    }
}
