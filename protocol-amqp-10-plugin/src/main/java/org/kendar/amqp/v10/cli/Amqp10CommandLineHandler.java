package org.kendar.amqp.v10.cli;

import org.kendar.amqp.v10.Amqp10ProtocolSettings;
import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

@Extension
@TpmService(tags = "amqp10")
public class Amqp10CommandLineHandler extends NetworkProtocolCommandLineHandler implements ExtensionPoint {
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
        return "amqp10";
    }

    @Override
    public String getDescription() {
        return "Amqp 1.0 Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new Amqp10ProtocolSettings();
    }
}
