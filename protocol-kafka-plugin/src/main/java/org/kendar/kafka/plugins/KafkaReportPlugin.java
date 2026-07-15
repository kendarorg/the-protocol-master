package org.kendar.kafka.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicReportPlugin;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Reports Kafka activity to the global report plugin. */
@Extension
@TpmService(tags = "kafka")
public class KafkaReportPlugin extends BasicReportPlugin<PluginSettings> {
    public KafkaReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "kafka";
    }
}
