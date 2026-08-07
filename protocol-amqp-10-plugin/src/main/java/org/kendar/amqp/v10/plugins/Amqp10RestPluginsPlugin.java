package org.kendar.amqp.v10.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

/** Exposes the AMQP 1.0 plugins over REST. */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10RestPluginsPlugin extends BasicRestPluginsPlugin {
    public Amqp10RestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "amqp10";
    }
}
