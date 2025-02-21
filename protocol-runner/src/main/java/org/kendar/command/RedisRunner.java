package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.GlobalSettings;

import java.util.ArrayList;
import java.util.List;

@TpmService
public class RedisRunner extends CommonRunner {

    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new ByteProtocolSettings();
        settings.setProtocol(getId());
        var recording = new BasicAysncRecordPluginSettings();
        var replaying = new BasicAysncReplayPluginSettings();
        List<CommandOption> commandOptionList = getCommonOptions(globalSettings, settings, recording, replaying, new ArrayList<>());
        return CommandOptions.of(getId())
                .withDescription("Redis Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }

    @Override
    protected String getConnectionDescription() {
        return "redis://localhost:5372";
    }

    @Override
    public String getDefaultPort() {
        return "6379";
    }


    @Override
    public String getId() {
        return "redis";
    }
}
