package org.kendar.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

public abstract class RecordPluginCli extends BasicPluginCli {
    protected String getPluginName(){return "record-plugin";}
    protected String getPluginDescription(){return "Record calls";}

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("itc", "Ignore Trivial Calls, default true")
                .withLong("ignoreTrivialCalls")
                .withCallback((s) -> ((BasicRecordPluginSettings)settings).setIgnoreTrivialCalls(true)));
        return options.toArray(new CommandOption[0]);
    }

    @Override
    protected PluginSettings buildPluginSettings() {
        return new BasicRecordPluginSettings();
    }
}
