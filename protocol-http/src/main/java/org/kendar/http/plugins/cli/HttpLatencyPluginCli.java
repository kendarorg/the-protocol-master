package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.HttpLatencyPluginSettings;
import org.kendar.plugins.cli.BasicPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpLatencyPluginCli extends BasicPluginCli {

    @Override
    public PluginSettings buildPluginSettings() {
        return new HttpLatencyPluginSettings();
    }

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
                .withCallback((s) -> ((HttpLatencyPluginSettings) settings).setMaxMs(Integer.parseInt(s))));
        options.add(CommandOption.of("min", "Min milliseconds latency, default 0")
                .withCallback((s) -> ((HttpLatencyPluginSettings) settings).setMinMs(Integer.parseInt(s))));
        options.add(CommandOption.of("ewh", "Modify latency on following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("errorWhere")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpLatencyPluginSettings) settings).setLatencySites(s)));
        return options.toArray(new CommandOption[0]);
    }

}
