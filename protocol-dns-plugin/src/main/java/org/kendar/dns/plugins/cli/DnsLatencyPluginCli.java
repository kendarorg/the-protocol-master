package org.kendar.dns.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicLatencyPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "dns")
public class DnsLatencyPluginCli extends BasicLatencyPluginCli {

}
