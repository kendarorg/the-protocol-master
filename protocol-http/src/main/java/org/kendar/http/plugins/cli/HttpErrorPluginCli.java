package org.kendar.http.plugins.cli;

import org.kendar.cli.CommandOption;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpErrorPluginSettings;
import org.kendar.plugins.cli.BasicPluginCli;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;

@TpmService(tags = "http")
public class HttpErrorPluginCli extends BasicPluginCli {

    @Override
    public PluginSettings buildPluginSettings() {
        return new HttpErrorPluginSettings();
    }

    @Override
    protected String getPluginName() {
        return "error-plugin";
    }

    @Override
    protected String getPluginDescription() {
        return "Inject specific errors";
    }

    @Override
    protected CommandOption[] buildPluginOptions(PluginSettings settings) {
        var options = new ArrayList<CommandOption>();
        options.add(CommandOption.of("msg", "Error message")
                .withLong("errorMessage")
                .withCallback((s) -> ((HttpErrorPluginSettings) settings).setErrorMessage(s)));
        options.add(CommandOption.of("pca", "Percent calls touched, default 50, meaning 50%")
                .withLong("percentAction")
                .withCallback((s) -> ((HttpErrorPluginSettings) settings).setPercentAction(Integer.parseInt(s))));
        options.add(CommandOption.of("err", "Error code to show, default Error")
                .withLong("errorCode")
                .withCallback((s) -> ((HttpErrorPluginSettings) settings).setShowError(Integer.parseInt(s))));
        options.add(CommandOption.of("t", "Generate erros on following websites @\r\n" +
                        "@REGEX or  STARTWITH. Default anything")
                .withLong("target")
                .asMultiple()
                .withMultiCallback((s) -> ((HttpErrorPluginSettings) settings).setTarget(s)));
        return options.toArray(new CommandOption[0]);
    }

}
