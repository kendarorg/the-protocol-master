package org.kendar.redis.plugins;

import org.kendar.annotations.di.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;

import java.util.Map;

@TpmService(tags = "redis")
public class RedisReportPlugin extends ReportPlugin<PluginSettings> {
    public RedisReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Resp3Message in, Resp3Response out) {
        if (isActive()) {
            var data = mapper.toJsonNode(in.getData());
            if (data.isArray()) {
                if (data.get(0).textValue().equalsIgnoreCase("publish")) {
                    var context = pluginContext.getContext();
                    var connectionId = context.getContextId();
                    var duration = System.currentTimeMillis() - pluginContext.getStart();
                    EventsQueue.send(new ReportDataEvent(
                            getInstanceId(),
                            getProtocol(),
                            String.format("%s:%s:%s", data.get(0).textValue(), data.get(1).textValue(), data.get(2).textValue()),
                            connectionId,
                            pluginContext.getStart(),
                            duration,
                            Map.of()
                    ));
                } else if (data.get(0).textValue().equalsIgnoreCase("subscribe")) {
                    var context = pluginContext.getContext();
                    var connectionId = context.getContextId();
                    var duration = System.currentTimeMillis() - pluginContext.getStart();
                    EventsQueue.send(new ReportDataEvent(
                            getInstanceId(),
                            getProtocol(),
                            String.format("%s:%s", data.get(0).textValue(), data.get(1).textValue()),
                            connectionId,
                            pluginContext.getStart(),
                            duration,
                            Map.of()
                    ));
                }
            }
        }

        return false;
    }

    @Override
    public String getProtocol() {
        return "redis";
    }
}
