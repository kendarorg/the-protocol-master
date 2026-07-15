package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Exposes the Kafka plugins over REST. */
@Extension
@TpmService(tags = "kafka")
public class KafkaRestPluginsPlugin extends BasicRestPluginsPlugin {
    public KafkaRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }
}
