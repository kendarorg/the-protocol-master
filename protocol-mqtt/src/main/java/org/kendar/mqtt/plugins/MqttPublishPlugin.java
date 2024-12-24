package org.kendar.mqtt.plugins;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.apis.MqttPublishPluginApis;
import org.kendar.mqtt.fsm.PublishAck;
import org.kendar.mqtt.fsm.PublishRec;
import org.kendar.mqtt.fsm.PublishRel;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MqttPublishPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, PublishRec in, PublishRel out) {
        if(isActive()) {
            var ctx = pluginContext.getContext().getContextId();
            var pubRel = expectPubRel.get(ctx);
            if(pubRel != null && pubRel.getPacketIdentifier()==in.getPacketIdentifier()) {
                out.setReasonCode((byte) 0);
                out.setPacketIdentifier(in.getPacketIdentifier());
                out.setFullFlag(pubRel.getFullFlag());
                expectPubRel.remove(ctx);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, PublishAck in, Object out) {
        if(isActive()) {
            var ctx = pluginContext.getContext().getContextId();
            var pubRel = expectPubAck.get(ctx);
            if(pubRel != null && pubRel.getPacketIdentifier()==in.getPacketIdentifier()) {
                expectPubAck.remove(ctx);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public String getId() {
        return "publish-plugin";
    }

    @Override
    protected ProtocolPluginApiHandler buildApiHandler() {
        return new MqttPublishPluginApis(this, getId(), getInstanceId());
    }
    private ConcurrentHashMap<Integer,PublishRel> expectPubRel = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer,PublishAck> expectPubAck = new ConcurrentHashMap<>();

    public void expectPubRec(MqttContext context, PublishRel pubRel) {
        expectPubRel.put(context.getContextId(),pubRel);
    }

    public void expectPubAck(MqttContext context, PublishAck pubRel) {
        expectPubAck.put(context.getContextId(),pubRel);
    }
}
