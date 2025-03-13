package org.kendar.mqtt.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mqtt")
public class MqttLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public MqttLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }

}
