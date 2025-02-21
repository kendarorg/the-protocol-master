package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public abstract class ReplayPluginCli  extends BasicPluginCli {
    protected String getPluginName(){return "replay-plugin";}
    protected String getPluginDescription(){return "Replay calls";}

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("rpc", "Respect call durations, default false")
                .withLong("respectCallDurations")
                .withCallback((s) -> ((BasicReplayPluginSettings)settings).setRespectCallDuration(true)));
        options.add(CommandOption.of("itc", "Ignore Trivial Calls, default true")
                .withLong("ignoreTrivialCalls")
                .withCallback((s) -> ((BasicReplayPluginSettings)settings).setIgnoreTrivialCalls(true)));
        options.add(CommandOption.of("bx", "Block external calls, default true")
                .withLong("blockExternalCalls")
                .withCallback((s) -> ((BasicReplayPluginSettings)settings).setIgnoreTrivialCalls(true)));
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new BasicReplayPluginSettings();
    }
}
