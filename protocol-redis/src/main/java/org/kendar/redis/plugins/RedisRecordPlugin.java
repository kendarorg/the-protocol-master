package org.kendar.redis.plugins;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.kendar.events.EventsQueue;
import org.kendar.events.WriteItemEvent;
import org.kendar.plugins.RecordPlugin;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToWrite;

import java.util.Map;

public class RedisRecordPlugin extends RecordPlugin<BasicAysncRecordPluginSettings> {
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
    public Class<?> getSettingClass() {
        return BasicAysncRecordPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "redis";
    }

    @Override
    public Map<String, String> buildTag(StorageItem item) {
        if (item.getInput() != null && ArrayNode.class.isAssignableFrom(item.getInput().getClass())) {
            var input = (ArrayNode) item.getInput();
            if (input.size() >= 2) {

                if (input.get(0).asText().equalsIgnoreCase("SUBSCRIBE")) {
                    return Map.of("queue", input.get(1).asText());
                } else if (input.get(0).asText().equalsIgnoreCase("MESSAGE")) {
                    return Map.of("queue", input.get(1).asText());
                }
            }
        }
        if (item.getOutput() != null && ArrayNode.class.isAssignableFrom(item.getOutput().getClass())) {
            var out = (ArrayNode) item.getOutput();
            if (out.size() >= 2) {

                if (out.get(0).asText().equalsIgnoreCase("SUBSCRIBE")) {
                    return Map.of("queue", out.get(1).asText());
                } else if (out.get(0).asText().equalsIgnoreCase("MESSAGE")) {
                    return Map.of("queue", out.get(1).asText());
                }
            }
        }
        return Map.of();
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
