package org.kendar.cli;

import org.kendar.plugins.cli.BasicPluginCli;
import org.kendar.settings.JdbcRewritePluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public class BasicJdbcForwardPluginCli extends BasicPluginCli {
    @Override
    protected String getPluginName() {
        return "jdbc-forward-plugin";
    }

    @Override
    protected String getPluginDescription() {
        return "Forward requests to specific host/db";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new JdbcRewritePluginSettings();
    }
}
