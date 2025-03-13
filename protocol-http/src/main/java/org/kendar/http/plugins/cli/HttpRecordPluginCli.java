package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpRecordPluginSettings;
import org.kendar.plugins.cli.AsyncRecordPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpRecordPluginCli extends AsyncRecordPluginCli {

    @Override
    protected PluginSettings buildPluginSettings() {
        return new HttpRecordPluginSettings();
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("ret", "Remove e-tags, default false")
                .withLong("removeETags")
                .withCallback((s) -> ((HttpRecordPluginSettings) settings).setRemoveEtags(true)));
        options.add(CommandOption.of("rwh", "Record following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("recordWhat")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpRecordPluginSettings) settings).setTarget(s)));
        return options.toArray(new CommandOption[0]);
    }

}
