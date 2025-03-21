package org.kendar.redis.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.PluginException;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.util.List;
import java.util.Map;

@TpmService(tags = "redis")
public class RedisReplayPlugin extends BasicReplayPlugin<BasicAysncReplayPluginSettings> {
    protected static final JsonMapper mapper = new JsonMapper();

    public RedisReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }


    @Override
    public Class<?> getSettingClass() {
        return BasicAysncReplayPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "redis";
    }

    @Override
    protected void buildState(PluginContext pluginContext, ProtoContext context, Object in, Object outObj, Object toread, LineToRead lineToRead) {
        var out = mapper.toJsonNode(outObj);
        ((Resp3Response) toread).execute(new Resp3Message(context, null, out));
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> storageItems) {
        Sleeper.sleep(10);
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
            var type = item.getOutputType();

            int connectionId = item.getConnectionId();
            if (type.equalsIgnoreCase("RESPONSE")) {
                var ctx = context.getDescriptor().getContextsCache().get(connectionId);
                ReturnMessage fr = new Resp3Message(ctx, null, out);
                ctx.write(fr);
            } else {
                throw new PluginException("MISSING RESPONSE_CLASS");
            }

        }
    }


    protected Map<String, String> getContextTags(ProtoContext context) {
        if (context.getValue("QUEUE") != null) {
            return Map.of("queue", (String) context.getValue("QUEUE"));
        }
        return Map.of();
    }

    @Override
    protected boolean verifyContentRepeatable(CompactLine index) {
        return index.getTags().containsKey("repeatable");
    }
}
