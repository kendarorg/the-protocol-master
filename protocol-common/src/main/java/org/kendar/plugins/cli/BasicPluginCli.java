package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.PluginCommandLineHandler;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.List;

public abstract class BasicPluginCli implements PluginCommandLineHandler {
    protected abstract String getPluginName();

    protected abstract String getPluginDescription();

    public void setup(List<CommandOption> protocolOptions, ProtocolSettings settings) {
        var pluginSettings = buildPluginSettings();
        var pluginName = getPluginName();
        var shortName = pluginName.replace("-plugin", "").replace("plugin", "");
        protocolOptions.add(CommandOption.of(shortName, getPluginDescription())
                .withLong(getPluginName())
                .withCommandOptions(buildPluginOptions(pluginSettings))
                .withCallback(s -> {
                    pluginSettings.setActive(true);
                    settings.getPlugins().put(getPluginName(), pluginSettings);
                }));
    }

    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        return new CommandOption[]{};
    }

    protected PluginSettings buildPluginSettings() {
        return new PluginSettings();
    }
}
