package org.kendar.redis.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.WriteItemEvent;
import org.kendar.plugins.RecordPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;

public class RedisRecordPlugin extends RecordPlugin<BasicRecordPluginSettings> {
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
        var id = (long) pluginContext.getTags().get("id");
        var storageItem = new StorageItem(pluginContext.getContextId(),
                null,
                mapper.toJsonNode(out),
                duration, pluginContext.getType(),
                pluginContext.getCaller(),
                null,
                "RESPONSE");
        var tags = buildTag(storageItem);
        var compactLine = new CompactLine(storageItem, () -> tags);
        EventsQueue.send(new WriteItemEvent(new LineToWrite(getInstanceId(), storageItem, compactLine, id)));
    }
}
