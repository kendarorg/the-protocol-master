package org.kendar.amqp.v09;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;

@TpmService(tags = "amqp091")
public class AmqpProtocolSettings extends ByteProtocolSettingsWithLogin {
}
