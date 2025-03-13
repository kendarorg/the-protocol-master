package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpLatencyPluginSettings;
import org.kendar.plugins.cli.BasicLatencyPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpLatencyPluginCli extends BasicLatencyPluginCli {

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
        for(var opt:super.buildPluginOptions(settings)){
            options.add(opt);
        }
        options.add(CommandOption.of("t", "Modify latency on following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("target")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpLatencyPluginSettings) settings).setTarget(s)));
        return options.toArray(new CommandOption[0]);
    }

}
