package org.kendar.amqp.v10.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Injects network errors (dropped connections) into AMQP 1.0 traffic. */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10NetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public Amqp10NetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp10";
    }
}
