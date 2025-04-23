package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.BasicAsyncReplayPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class AsyncReplayPluginCli extends ReplayPluginCli {

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<>(List.of(super.buildPluginOptions(settings)));
        options.add(CommandOption.of("rcs", "Reset all connections when starting, default false")
                .withLong("resetConnectionsOnStartup")
                .withCallback((s) -> ((BasicAsyncReplayPluginSettings) settings).setResetConnectionsOnStart(true)));
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new BasicAsyncReplayPluginSettings();
    }
}
