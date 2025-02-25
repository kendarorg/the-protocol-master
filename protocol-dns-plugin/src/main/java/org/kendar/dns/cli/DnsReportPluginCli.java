package org.kendar.dns.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicPluginCli;

@TpmService(tags = "dns")
public class DnsReportPluginCli extends BasicPluginCli {
    protected String getPluginName() {
        return "report-plugin";
    }

    protected String getPluginDescription() {
        return "Send 'report' events to global report plugin";
    }
}
