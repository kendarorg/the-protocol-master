package org.kendar.mssql.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.BasicNetworkErrorPluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "mssql")
public class MssqlNetworErrorPluginCli extends BasicNetworkErrorPluginCli {

}
