package org.kendar.redis.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "redis")
public class RedisRestPluginsPlugin extends BasicRestPluginsPlugin {
    public RedisRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "redis";
    }
}
