package org.kendar.amqp.v09.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "amqp091")
public class AmqpLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public AmqpLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }

}
