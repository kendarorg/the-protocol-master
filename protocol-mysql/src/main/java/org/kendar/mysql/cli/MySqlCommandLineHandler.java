package org.kendar.mysql.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.mysql.MySqlProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

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
        var settings = (JdbcProtocolSettings) genericSettings;
        options.add(CommandOption.of("js", "Force schema name")
                .withLong("schema")
                .withMandatoryParameter()
                .withCallback(settings::setForceSchema));
        return options;
    }
}
