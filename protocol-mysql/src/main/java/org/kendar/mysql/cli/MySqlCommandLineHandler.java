package org.kendar.mysql.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.mysql.MySqlProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.List;

@TpmService(tags = "mysql")
public class MySqlCommandLineHandler extends NetworkProtocolCommandLineHandler {
    @Override
    protected String getConnectionDescription() {
        return "jdbc:mysql://localhost:3306";
    }


    @Override
    protected String getDefaultPort() {
        return "3306";
    }

    @Override
    public String getId() {
        return "mysql";
    }

    @Override
    public String getDescription() {
        return "MySQL protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new MySqlProtocolSettings();
    }

    @Override
    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        var options = super.prepareCustomOptions(globalSettings, genericSettings);
        var settings = (MySqlProtocolSettings) genericSettings;
        options.add(CommandOption.of("js", "Force schema name")
                .withLong("schema")
                .withMandatoryParameter()
                .withCallback(settings::setForceSchema));
        options.add(CommandOption.of("tls", "Enable TLS for MySQL connection")
                .withLong("useTls")
                .withCallback(s -> {
                    settings.setUseTls(true);
                }));
        return options;
    }
}
