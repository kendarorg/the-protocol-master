package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpReportPluginSettings;
import org.kendar.plugins.cli.BasicPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpReportPluginCli extends BasicPluginCli {
    @Override
    public PluginSettings buildPluginSettings() {
        return new HttpReportPluginSettings();
    }

    protected String getPluginName() {
        return "report-plugin";
    }

    protected String getPluginDescription() {
        return "Send 'report' events to global report plugin";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("ignTpm", "Do not send TPM calls reports, default true")
                .withLong("percentAction")
                .withCallback((s) -> ((HttpReportPluginSettings) settings).setIgnoreTpm(Boolean.parseBoolean(s))));
        return options.toArray(new CommandOption[0]);
    }
}
