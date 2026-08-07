package org.kendar.amqp.v10;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

@Extension
@TpmService(tags = "amqp10")
public class Amqp10ProtocolSettings extends ByteProtocolSettingsWithLogin implements ExtensionPoint {
    public Amqp10ProtocolSettings() {
        setProtocol("amqp10");
    }
}
