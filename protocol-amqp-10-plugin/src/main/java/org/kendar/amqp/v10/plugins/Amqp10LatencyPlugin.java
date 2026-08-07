package org.kendar.amqp.v10.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Injects artificial latency into AMQP 1.0 traffic. */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10LatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public Amqp10LatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp10";
    }
}
