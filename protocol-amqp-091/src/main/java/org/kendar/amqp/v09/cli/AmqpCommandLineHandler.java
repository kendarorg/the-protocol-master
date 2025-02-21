package org.kendar.amqp.v09.cli;

import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.ProtocolSettings;

@TpmService(tags = "amqp091")
public class AmqpCommandLineHandler extends NetworkProtocolCommandLineHandler {
    @Override
    protected String getConnectionDescription() {
        return "amqp://localhost:5672";
    }


    @Override
    protected String getDefaultPort() {
        return "5672";
    }

    @Override
    public String getId() {
        return "amqp091";
    }

    @Override
    public String getDescription() {
        return "Amqp 0.9.1 Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new ByteProtocolSettingsWithLogin();
    }
}
