package org.kendar.cli;

import org.kendar.plugins.cli.BasicPluginCli;
import org.kendar.settings.JdbcRewritePluginSettings;
import org.kendar.settings.PluginSettings;

public class BasicJdbcForwardPluginCli extends BasicPluginCli {
    @Override
    protected String getPluginName() {
        return "jdbc-forward";
    }

    @Override
    protected String getPluginDescription() {
        return "Forward requests to specific host/db";
    }


    @Override
    protected PluginSettings buildPluginSettings() {
        return new JdbcRewritePluginSettings();
    }
}
