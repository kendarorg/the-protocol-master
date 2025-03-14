package org.kendar.amqp.v09.plugins;

import org.kendar.amqp.v09.plugins.apis.AmqpPublishPluginApis;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

import java.util.List;

@TpmService(tags = "amqp091")
public class AmqpPublishPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    private final MultiTemplateEngine resolversFactory;

    public AmqpPublishPlugin(JsonMapper mapper, MultiTemplateEngine resolversFactory) {
        super(mapper);
        this.resolversFactory = resolversFactory;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.NONE);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        return false;
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }

    @Override
    public String getId() {
        return "publish-plugin";
    }

    @Override
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new AmqpPublishPluginApis(this, getId(), getInstanceId(),resolversFactory));
    }
}
