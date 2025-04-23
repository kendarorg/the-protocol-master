package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.BasicAsyncRecordPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public abstract class AsyncRecordPluginCli extends RecordPluginCli {


    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<>(List.of(super.buildPluginOptions(settings)));
        options.add(CommandOption.of("rcs", "Reset all connections when starting, default false")
                .withLong("resetConnectionsOnStartup")
                .withCallback((s) -> ((BasicAsyncRecordPluginSettings) settings).setResetConnectionsOnStart(true)));
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new BasicAsyncRecordPluginSettings();
    }
}
