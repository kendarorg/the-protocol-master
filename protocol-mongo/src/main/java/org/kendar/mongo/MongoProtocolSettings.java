package org.kendar.mongo;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;

@TpmService(tags = "mongodb")
public class MongoProtocolSettings extends ByteProtocolSettingsWithLogin {
    public MongoProtocolSettings() {
        setProtocol("mongodb");
    }
}
