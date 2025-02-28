package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public class BasicLatencyPluginCli extends BasicPluginCli {
    @Override
    protected String getPluginName() {
        return "latency-plugin";
    }

    @Override
    protected String getPluginDescription() {
        return "Add latency to calls";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("max", "Max milliseconds latency, default 0")
                .withCallback((s) -> ((LatencyPluginSettings) settings).setMaxMs(Integer.parseInt(s))));
        options.add(CommandOption.of("min", "Min milliseconds latency, default 0")
                .withCallback((s) -> ((LatencyPluginSettings) settings).setMinMs(Integer.parseInt(s))));
        options.add(CommandOption.of("pc", "Percent calls touched, default 50, meaning 50%")
                .withLong("percentAction")
                .withCallback((s) -> ((LatencyPluginSettings) settings).setPercentAction(Integer.parseInt(s))));

        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new LatencyPluginSettings();
    }
}
