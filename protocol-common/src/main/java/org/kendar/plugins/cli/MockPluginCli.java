package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public abstract class MockPluginCli extends BasicPluginCli {
    protected String getPluginName(){return "mock-plugin";}
    protected String getPluginDescription(){return "Mock certain service requests";}

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new BasicMockPluginSettings();
    }
}
