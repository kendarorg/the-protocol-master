package org.kendar.amqp.v09.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "amqp091")
public class AmqpNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public AmqpNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }
}
