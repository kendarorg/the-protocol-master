package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpErrorPluginSettings;
import org.kendar.http.plugins.settings.HttpLatencyPluginSettings;
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
        options.add(CommandOption.of("pc", "Percent calls touched, default 50, meaning 50%")
                .withLong("percentAction")
                .withCallback((s) -> ((HttpErrorPluginSettings) settings).setPercentAction(Integer.parseInt(s))));
        options.add(CommandOption.of("t", "Modify latency on following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("target")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpLatencyPluginSettings) settings).setTarget(s)));
        return options.toArray(new CommandOption[0]);
    }

}
