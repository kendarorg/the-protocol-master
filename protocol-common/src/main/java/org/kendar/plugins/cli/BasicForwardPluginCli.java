package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.BasicForwardPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public class BasicForwardPluginCli extends BasicPluginCli {
    @Override
    protected String getPluginName() {
        return "forward-plugin";
    }

    @Override
    protected String getPluginDescription() {
        return "Forward requests to specific server (including passwords)";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new BasicForwardPluginSettings();
    }
}
