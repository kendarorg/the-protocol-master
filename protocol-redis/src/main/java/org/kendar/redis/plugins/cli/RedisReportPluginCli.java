package org.kendar.redis.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;

@TpmService(tags = "redis")
public class RedisReportPluginCli extends BasicPluginCli {
    protected String getPluginName() {
        return "report-plugin";
    }

    protected String getPluginDescription() {
        return "Send 'report' events to global report plugin";
    }
}
