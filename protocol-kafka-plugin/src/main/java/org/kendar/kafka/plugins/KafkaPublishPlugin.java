package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.plugins.apis.KafkaPublishPluginApis;
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
 * Produces a message for real through the proxy's upstream connection (a v9
 * Produce request built with {@link org.kendar.kafka.utils.KafkaProduceEncoder} —
 * <b>not</b> synthetic record-batch injection, protocol-kafka.md §7). Exposes REST
 * APIs + a JTE panel via {@link KafkaPublishPluginApis}.
 */
@Extension
@TpmService(tags = "kafka")
public class KafkaPublishPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    private final MultiTemplateEngine resolversFactory;

    public KafkaPublishPlugin(JsonMapper mapper, MultiTemplateEngine resolversFactory) {
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
        return "kafka";
    }

    @Override
    public String getId() {
        return "publish-plugin";
    }

    @Override
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new KafkaPublishPluginApis(this, getId(), getInstanceId(), resolversFactory));
    }
}
