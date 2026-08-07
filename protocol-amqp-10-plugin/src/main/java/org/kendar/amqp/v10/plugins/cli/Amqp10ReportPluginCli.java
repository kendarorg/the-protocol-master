package org.kendar.amqp.v10.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "amqp10")
public class Amqp10ReportPluginCli extends BasicPluginCli {
    protected String getPluginName() {
        return "report-plugin";
    }

    protected String getPluginDescription() {
        return "Send 'report' events to global report plugin";
    }
}
