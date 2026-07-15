package org.kendar.amqp.v10.plugins;

import org.kendar.amqp.v10.plugins.apis.Amqp10PublishPluginApis;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

import java.util.List;

/**
 * Injects AMQP 1.0 {@code transfer} deliveries onto connected consumer links
 * (the v09 {@code AmqpPublishPlugin} analog). Exposes REST APIs + a JTE panel via
 * {@link Amqp10PublishPluginApis}; the delivery links are correlated by the
 * {@code attach} state into the context (see
 * {@link org.kendar.amqp.v10.utils.ReceiverLink}).
 */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10PublishPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    private final MultiTemplateEngine resolversFactory;

    public Amqp10PublishPlugin(JsonMapper mapper, MultiTemplateEngine resolversFactory) {
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
        return "amqp10";
    }

    @Override
    public String getId() {
        return "publish-plugin";
    }

    @Override
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new Amqp10PublishPluginApis(this, getId(), getInstanceId(), resolversFactory));
    }
}
