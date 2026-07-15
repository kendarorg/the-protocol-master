package org.kendar.amqp.v10.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "amqp10")
public class Amqp10PublishPluginCli extends BasicPluginCli {
    protected String getPluginName() {
        return "publish-plugin";
    }

    protected String getPluginDescription() {
        return "Publish asynchronous calls";
    }
}
