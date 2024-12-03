package org.kendar.amqp.v09.plugins;

import org.bouncycastle.util.encoders.Base64;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpenOk;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;

import java.util.Map;

public class AmqpReportPlugin extends ReportPlugin< PluginSettings> {


    @Override
    public String getProtocol() {
        return "amqp091";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, ConnectionOpen in, ConnectionOpenOk out) {
        if(!isActive())return false;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                String.format("CONNECT"),
                connectionId,
                pluginContext.getStart(),
                0,
                Map.of()
        ));
        return false;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, BodyFrame in, Object out) {
        if(!isActive())return false;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        var channel = in.getChannel();
        var routingKey = context.getValue("BASIC_PUBLISH_RK_" + in.getChannel());
        var exchange = context.getValue("BASIC_PUBLISH_XC_" + in.getChannel());
        var payload= "";
        if(in.getContentString()!=null && in.getContentString().length()>0){
            payload = in.getContentString();
        }else if(in.getContentBytes()!=null && in.getContentBytes().length>0){
            try {
                payload = new String(in.getContentBytes());
                var converted = payload.getBytes("UTF-8");
                if(in.getContentBytes().length==converted.length){
                    for(var i=0;i<converted.length;i++){
                        if(converted[i]!=in.getContentBytes()[i]){
                            payload=Base64.toBase64String(in.getContentBytes());
                            break;
                        }
                    }
                }
            }catch (Exception e){
                payload = Base64.toBase64String(in.getContentBytes());
            }
        }

        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                String.format("SEND:%s:%s",exchange,routingKey),
                connectionId,
                pluginContext.getStart(),
                duration,
                Map.of("body",payload)
        ));
        return false;
    }
}
