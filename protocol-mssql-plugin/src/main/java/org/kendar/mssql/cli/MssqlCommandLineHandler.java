package org.kendar.mssql.cli;

import org.kendar.cli.CommandOption;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.mssql.MssqlProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.util.List;

@Extension
@TpmService(tags = "mssql")
public class MssqlCommandLineHandler extends NetworkProtocolCommandLineHandler implements ExtensionPoint {
    @Override
    protected String getConnectionDescription() {
        return "jdbc:sqlserver://localhost:1433;encrypt=false;databaseName=master";
    }


    @Override
    protected String getDefaultPort() {
        return "1433";
    }

    @Override
    public String getId() {
        return "mssql";
    }

    @Override
    public String getDescription() {
        return "MS SQL Server protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new MssqlProtocolSettings();
    }

    @Override
    protected List<CommandOption> prepareCustomOptions(GlobalSettings globalSettings, ProtocolSettings genericSettings) {
        var options = super.prepareCustomOptions(globalSettings, genericSettings);
        var settings = (MssqlProtocolSettings) genericSettings;
        options.add(CommandOption.of("js", "Force schema name")
                .withLong("schema")
                .withMandatoryParameter()
                .withCallback(settings::setForceSchema));
        options.add(CommandOption.of("tls", "Enable TLS for MSSQL connection")
                .withLong("useTls")
                .withCallback(s -> {
                    settings.setUseTls(true);
                }));
        return options;
    }
}
