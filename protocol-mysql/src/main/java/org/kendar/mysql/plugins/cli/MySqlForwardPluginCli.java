package org.kendar.mysql.plugins.cli;

import org.kendar.cli.BasicJdbcForwardPluginCli;
import org.kendar.di.annotations.TpmService;

@TpmService(tags = "mysql")
public class MySqlForwardPluginCli extends BasicJdbcForwardPluginCli {
}
