package org.kendar.redis.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "redis")
public class RedisNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public RedisNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "redis";
    }
}
