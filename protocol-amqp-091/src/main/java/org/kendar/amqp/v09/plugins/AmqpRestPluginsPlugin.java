package org.kendar.amqp.v09.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "amqp091")
public class AmqpRestPluginsPlugin extends BasicRestPluginsPlugin {
    public AmqpRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp091";
    }
}
