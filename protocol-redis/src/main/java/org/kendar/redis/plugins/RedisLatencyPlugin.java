package org.kendar.redis.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "redis")
public class RedisLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public RedisLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "redis";
    }

}
