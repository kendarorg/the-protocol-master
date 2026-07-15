package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Injects artificial latency into Kafka traffic. */
@Extension
@TpmService(tags = "kafka")
public class KafkaLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public KafkaLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }
}
