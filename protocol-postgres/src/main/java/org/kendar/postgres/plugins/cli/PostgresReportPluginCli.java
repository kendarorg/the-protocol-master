package org.kendar.postgres.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;

@TpmService(tags = "postgres")
public class PostgresReportPluginCli extends BasicPluginCli {
    protected String getPluginName(){return "report-plugin";}
    protected String getPluginDescription(){return "Send 'report' events to global report plugin";}
}
