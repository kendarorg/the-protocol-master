package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpLatencyPluginSettings;
import org.kendar.plugins.cli.BasicLatencyPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.Arrays;

@TpmService(tags = "http")
public class HttpLatencyPluginCli extends BasicLatencyPluginCli {

    @Override
    public PluginSettings buildPluginSettings() {
        return new HttpLatencyPluginSettings();
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.addAll(Arrays.asList(super.buildPluginOptions(settings)));
        options.add(CommandOption.of("t", "Modify latency on following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("target")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpLatencyPluginSettings) settings).setTarget(s)));
        return options.toArray(new CommandOption[0]);
    }

}
