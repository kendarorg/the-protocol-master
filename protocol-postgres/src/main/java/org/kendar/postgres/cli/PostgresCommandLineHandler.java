package org.kendar.postgres.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

import java.util.List;

@TpmService(tags = "postgres")
public class PostgresCommandLineHandler extends NetworkProtocolCommandLineHandler {

    @Override
    protected String getConnectionDescription() {
        return "jdbc:postgresql://localhost:5432/db?ssl=false";
    }


    @Override
    protected String getDefaultPort() {
        return "5432";
    }

    @Override
    public String getId() {
        return "postgres";
    }

    @Override
    public String getDescription() {
        return "PostgreSQL protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new JdbcProtocolSettings();
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
