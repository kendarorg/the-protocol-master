package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;

import java.util.List;

@TpmService
public class MongoRunner extends CommonRunner {

    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new ByteProtocolSettingsWithLogin();
        settings.setProtocol(getId());
        var recording = new BasicRecordPluginSettings();
        var replaying = new BasicReplayPluginSettings();
        List<CommandOption> commandOptionList = getCommonOptions(
                globalSettings, settings, recording, replaying, optionLoginPassword(settings));
        return CommandOptions.of(getId())
                .withDescription("MongoDb Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }

    @Override
    public String getId() {
        return "mongodb";
    }

    @Override
    public Class<?> getSettingsClass() {
        return ByteProtocolSettingsWithLogin.class;
    }

    @Override
    public String getDefaultPort() {
        return "27018";
    }

    @Override
    protected String getConnectionDescription() {
        return "mongodb://localhost:27018";
    }

}
