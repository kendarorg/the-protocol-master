package org.kendar.dns.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicNetworkErrorPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "dns")
public class DnsNetworErrorPluginCli extends BasicNetworkErrorPluginCli {
    
}
