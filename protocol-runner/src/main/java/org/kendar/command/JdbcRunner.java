package org.kendar.command;

import org.kendar.cli.CommandOption;
import org.kendar.cli.CommandOptions;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.List;

public class JdbcRunner extends CommonRunner {
    protected static final JsonMapper mapper = new JsonMapper();
    private final String protocol;

    public JdbcRunner(String id) {

        this.protocol = id;
    }


    @Override
    public String getDefaultPort() {
        return protocol.equalsIgnoreCase("mysql") ? "3306" : "5432";
    }

    @Override
    protected String getConnectionDescription() {
        return protocol.equalsIgnoreCase("mysql") ? "jdbc:mysql://localhost:3306" :
                "jdbc:postgresql://localhost:5432/db?ssl=false";
    }

    @Override
    public CommandOptions getOptions(GlobalSettings globalSettings) {
        var settings = new JdbcProtocolSettings();
        settings.setProtocol(getId());
        var recording = new BasicRecordPluginSettings();
        var replaying = new BasicReplayPluginSettings();
        var rewrite = new RewritePluginSettings();
        var extra = new ArrayList<CommandOption>();
        extra.addAll(optionLoginPassword(settings));
        extra.addAll(List.of(
                CommandOption.of("js", "Force schema name")
                        .withLong("schema")
                        .withMandatoryParameter()
                        .withCallback(settings::setForceSchema),
                CommandOption.of("rew", "Path of the rewrite queries file")
                        .withLong("rewrite")
                        .withCallback((s) -> {
                            settings.getPlugins().put("rewrite-plugin", rewrite);
                            rewrite.setActive(true);
                        })
        ));
        List<CommandOption> commandOptionList = getCommonOptions(globalSettings, settings, recording, replaying, extra);
        return CommandOptions.of(getId())
                .withDescription(getId() + " Protocol")
                .withOptions(
                        commandOptionList.toArray(new CommandOption[0])
                )
                .withCallback(s -> globalSettings.getProtocols().put(s, settings));
    }

    @Override
    public String getId() {
        return protocol;
    }

    @Override
    public Class<?> getSettingsClass() {
        return JdbcProtocolSettings.class;
    }


}


