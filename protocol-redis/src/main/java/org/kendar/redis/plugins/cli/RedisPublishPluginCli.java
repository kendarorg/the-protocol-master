package org.kendar.redis.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;

@TpmService(tags = "redis")
public class RedisPublishPluginCli extends BasicPluginCli {
    protected String getPluginName(){return "publish-plugin";}
    protected String getPluginDescription(){return "Publish asynchronous calls";}
}
