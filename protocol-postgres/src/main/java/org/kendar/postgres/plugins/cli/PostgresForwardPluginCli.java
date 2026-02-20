package org.kendar.postgres.plugins.cli;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicForwardPlugin;
import org.kendar.plugins.cli.BasicForwardPluginCli;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresForwardPluginCli extends BasicForwardPluginCli {

}
