package org.kendar.amqp.v09.plugins;

import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpenOk;
import org.kendar.annotations.di.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;

import java.util.Map;

@TpmService(tags = "amqp091")
public class AmqpReportPlugin extends ReportPlugin<PluginSettings> {


    public AmqpReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, ConnectionOpen in, ConnectionOpenOk out) {
        if (!isActive()) return false;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                "CONNECT",
                connectionId,
                pluginContext.getStart(),
                0,
                Map.of()
        ));
        return false;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, BodyFrame in, Object out) {
        if (!isActive()) return false;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        var channel = in.getChannel();
        var routingKey = context.getValue("BASIC_PUBLISH_RK_" + in.getChannel());
        var exchange = context.getValue("BASIC_PUBLISH_XC_" + in.getChannel());
        var payload = mapper.toHumanReadable(in.getContent());

        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                String.format("SEND:%s:%s", exchange, routingKey),
                connectionId,
                pluginContext.getStart(),
                duration,
                Map.of("body", payload,"channel",channel+"")
        ));
        return false;
    }
}
