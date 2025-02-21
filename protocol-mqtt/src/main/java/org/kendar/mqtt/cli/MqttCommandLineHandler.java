package org.kendar.mqtt.cli;

import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.ProtocolSettings;

@TpmService(tags = "mqtt")
public class MqttCommandLineHandler extends NetworkProtocolCommandLineHandler {
    @Override
    protected String getConnectionDescription() {
        return "tcp://localhost:1883";
    }


    @Override
    protected String getDefaultPort() {
        return "1883";
    }

    @Override
    public String getId() {
        return "mqtt";
    }

    @Override
    public String getDescription() {
        return "MQTT Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new ByteProtocolSettingsWithLogin();
    }
}
