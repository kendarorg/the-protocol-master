package org.kendar.mqtt.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;

@TpmService(tags = "mqtt")
public class MqttPublishPluginCli extends BasicPluginCli {
    protected String getPluginName(){return "publish-plugin";}
    protected String getPluginDescription(){return "Publish asynchronous calls";}
}
