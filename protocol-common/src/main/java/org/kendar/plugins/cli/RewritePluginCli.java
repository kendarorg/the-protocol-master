package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public abstract class RewritePluginCli extends BasicPluginCli {
    protected String getPluginName(){return "rewrite-plugin";}
    protected String getPluginDescription(){return "Rewrite the requests sent to the server";}

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new RewritePluginSettings();
    }
}
