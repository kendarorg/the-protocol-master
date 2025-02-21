package org.kendar.mqtt;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;

@TpmService(tags = "mqtt")
public class MqttProtocolSettings extends ByteProtocolSettingsWithLogin {
    public MqttProtocolSettings() {
        setProtocol("mqtt");
    }
}
