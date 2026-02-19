package org.kendar.postgres.plugins.cli;

import org.kendar.cli.BasicJdbcForwardPluginCli;
import org.kendar.di.annotations.TpmService;

@TpmService(tags = "postgres")
public class PostgresForwardPluginCli extends BasicJdbcForwardPluginCli {
}
