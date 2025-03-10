package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.HttpRateLimitPluginSettings;
import org.kendar.plugins.cli.BasicPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpRateLimitPluginCli extends BasicPluginCli {

    @Override
    public PluginSettings buildPluginSettings() {
        return new HttpRateLimitPluginSettings();
    }

    @Override
    protected String getPluginName() {
        return "rate-limit-plugin";
    }

    @Override
    protected String getPluginDescription() {
        return "Force rate limit plugin";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("rateLimit", "Rate limit, default 120")
                .withCallback((s) -> ((HttpRateLimitPluginSettings) settings).setRateLimit(Integer.parseInt(s))));
        options.add(CommandOption.of("resetTimeSeconds", "Reset time window in seconds")
                .withCallback((s) -> ((HttpRateLimitPluginSettings) settings).setResetTimeWindowSeconds(Integer.parseInt(s))));
        options.add(CommandOption.of("cpr", "Cost per request, default 2")
                .withCallback((s) -> ((HttpRateLimitPluginSettings) settings).setCostPerRequest(Integer.parseInt(s))));
        options.add(CommandOption.of("twh", "Generate throttle on following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("throttleWhere")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpRateLimitPluginSettings) settings).setLimitSites(s)));
        return options.toArray(new CommandOption[0]);
    }

}
