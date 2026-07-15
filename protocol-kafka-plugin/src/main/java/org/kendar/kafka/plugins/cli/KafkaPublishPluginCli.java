package org.kendar.kafka.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "kafka")
public class KafkaPublishPluginCli extends BasicPluginCli {
    protected String getPluginName() {
        return "publish-plugin";
    }

    protected String getPluginDescription() {
        return "Publish messages to a topic through the proxy connection";
    }
}
