package org.kendar.sample.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.kafka.messages.KafkaRawMessage;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

import java.util.List;

/**
 * Sample Kafka filter (parity with {@code Amqp091Filter}): an always-active
 * plugin that observes raw Kafka request frames as they pass through the proxy.
 * Returns {@code false} (never blocks) — it is a template for custom Kafka logic.
 */
@Extension
@TpmService(tags = "kafka")
public class KafkaFilter extends ProtocolPluginDescriptorBase<KafkaFilterSettings> implements AlwaysActivePlugin {

    public KafkaFilter(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public Class<?> getSettingClass() {
        return KafkaFilterSettings.class;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of();
    }

    @Override
    public String getId() {
        return "sample-kafka";
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, KafkaRawMessage in, Object out) {
        return false;
    }
}
