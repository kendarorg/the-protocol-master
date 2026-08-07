package org.kendar.mssql.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.cli.RewritePluginCli;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "mssql")
public class MssqlRewritePluginCli extends RewritePluginCli {

}
