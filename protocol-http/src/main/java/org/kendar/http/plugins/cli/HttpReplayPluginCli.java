package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.HttpReplayPluginSettings;
import org.kendar.plugins.cli.AsyncReplayPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpReplayPluginCli extends AsyncReplayPluginCli {
    @Override
    protected PluginSettings buildPluginSettings() {
        return new HttpReplayPluginSettings();
    }


    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("pwh", "Replay following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("replayWhat")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpReplayPluginSettings) settings).setMatchSites(s)));
        return options.toArray(new CommandOption[0]);
    }
}
