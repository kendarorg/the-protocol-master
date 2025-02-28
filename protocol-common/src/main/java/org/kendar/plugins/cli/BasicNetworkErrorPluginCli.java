package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public class BasicNetworkErrorPluginCli extends BasicPluginCli {
    @Override
    protected String getPluginName() {
        return "network-error-plugin";
    }

    @Override
    protected String getPluginDescription() {
        return "Change random bytes in the response data";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("pc", "Percent calls touched, default 50, meaning 50%")
                .withLong("percentAction")
                .withCallback((s) -> ((NetworkErrorPluginSettings) settings).setPercentAction(Integer.parseInt(s))));

        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new NetworkErrorPluginSettings();
    }
}
