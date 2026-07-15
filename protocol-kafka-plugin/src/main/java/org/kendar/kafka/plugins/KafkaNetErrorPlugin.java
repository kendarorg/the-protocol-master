package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Injects network errors (dropped connections) into Kafka traffic. */
@Extension
@TpmService(tags = "kafka")
public class KafkaNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public KafkaNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }
}
