package org.kendar.mqtt.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mqtt")
public class MqttRestPluginsPlugin extends BasicRestPluginsPlugin {
    public MqttRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }
}
