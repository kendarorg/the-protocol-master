package org.kendar.amqp.v09.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;

@TpmService(tags = "amqp091")
public class AmqpPublishPluginCli extends BasicPluginCli {
    protected String getPluginName() {
        return "publish-plugin";
    }

    protected String getPluginDescription() {
        return "Publish asynchronous calls";
    }
}
