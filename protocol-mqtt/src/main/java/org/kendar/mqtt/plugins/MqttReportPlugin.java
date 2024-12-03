package org.kendar.mqtt.plugins;

import org.bouncycastle.util.encoders.Base64;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mqtt.fsm.Publish;
import org.kendar.mqtt.fsm.PublishAck;
import org.kendar.mqtt.fsm.PublishRec;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;

import java.util.Map;

public class MqttReportPlugin extends ReportPlugin<PluginSettings> {
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Publish in, Object out) {
        handlePublish(pluginContext,in,0);
        return false;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Publish in, PublishAck out) {
        handlePublish(pluginContext,in,1);
        return false;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Publish in, PublishRec out) {
        handlePublish(pluginContext,in,2);
        return false;
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }



    private void handlePublish(PluginContext pluginContext, Publish in, int qos) {
        if(!isActive())return;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        var data= "";
        var payload = "";
        try {
            payload = new String(in.getPayload());
            var converted = payload.getBytes("UTF-8");
            if(in.getPayload().length==converted.length){
                for(var i=0;i<converted.length;i++){
                    if(converted[i]!=in.getPayload()[i]){
                        payload=Base64.toBase64String(in.getPayload());
                        break;
                    }
                }
            }
        }catch (Exception e){
            payload = Base64.toBase64String(in.getPayload());
        }

        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                String.format("SEND:%s:%s",in.getTopicName(),payload),
                connectionId,
                pluginContext.getStart(),
                duration,
                Map.of("qos",qos+"",
                        "payloadLength",in.getPayload().length+"")
        ));
    }
}
