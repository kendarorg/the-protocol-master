package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;

import java.util.List;

@TpmService
public class Amqp091Runner extends CommonRunner {
    @Override
    protected String getConnectionDescription() {
        return "amqp://localhost:5372";
    }


    @Override
    public String getDefaultPort() {
        return "5672";
    }


    @Override
    public String getId() {
        return "amqp091";
    }


    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new ByteProtocolSettingsWithLogin();
        settings.setProtocol(getId());
        var recording = new BasicAysncRecordPluginSettings();
        var replaying = new BasicAysncReplayPluginSettings();
        List<CommandOption> commandOptionList = getCommonOptions(globalSettings, settings, recording, replaying, optionLoginPassword(settings));
        return CommandOptions.of(getId())
                .withDescription("Amqp 0.9.1 Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }
}
