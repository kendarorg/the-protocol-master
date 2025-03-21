package org.kendar.mqtt.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.fsm.PublishAck;
import org.kendar.mqtt.fsm.PublishRec;
import org.kendar.mqtt.fsm.PublishRel;
import org.kendar.mqtt.plugins.apis.MqttPublishPluginApis;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@TpmService(tags = "mqtt")
public class MqttPublishPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    private final ConcurrentHashMap<Integer, PublishRel> expectPubRel = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PublishAck> expectPubAck = new ConcurrentHashMap<>();
    private final MultiTemplateEngine resolversFactory;

    public MqttPublishPlugin(JsonMapper mapper, MultiTemplateEngine resolversFactory) {
        super(mapper);
        this.resolversFactory = resolversFactory;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, PublishRec in, PublishRel out) {
        if (isActive()) {
            var ctx = pluginContext.getContext().getContextId();
            var pubRel = expectPubRel.get(ctx);
            if (pubRel != null && pubRel.getPacketIdentifier() == in.getPacketIdentifier()) {
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
        if (isActive()) {
            var ctx = pluginContext.getContext().getContextId();
            var pubRel = expectPubAck.get(ctx);
            if (pubRel != null && pubRel.getPacketIdentifier() == in.getPacketIdentifier()) {
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
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new MqttPublishPluginApis(this, getId(), getInstanceId(), resolversFactory));
    }

    public void expectPubRec(MqttContext context, PublishRel pubRel) {
        expectPubRel.put(context.getContextId(), pubRel);
    }

    public void expectPubAck(MqttContext context, PublishAck pubRel) {
        expectPubAck.put(context.getContextId(), pubRel);
    }
}
