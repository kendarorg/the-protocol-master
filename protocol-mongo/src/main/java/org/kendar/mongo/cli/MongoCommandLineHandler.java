package org.kendar.mongo.cli;

import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.ProtocolSettings;

@TpmService(tags = "mongodb")
public class MongoCommandLineHandler extends NetworkProtocolCommandLineHandler {
    @Override
    protected String getConnectionDescription() {
        return "mongodb://localhost:27018";
    }


    @Override
    protected String getDefaultPort() {
        return "27018";
    }

    @Override
    public String getId() {
        return "mongodb";
    }

    @Override
    public String getDescription() {
        return "MongoDB protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new ByteProtocolSettingsWithLogin();
    }
}
