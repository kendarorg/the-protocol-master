package org.kendar.mqtt.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mqtt")
public class MqttNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public MqttNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }
}
