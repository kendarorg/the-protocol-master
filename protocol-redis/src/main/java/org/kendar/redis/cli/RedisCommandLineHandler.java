package org.kendar.redis.cli;

import org.kendar.command.NetworkProtocolCommandLineHandler;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.ProtocolSettings;

@TpmService(tags = "redis")
public class RedisCommandLineHandler extends NetworkProtocolCommandLineHandler {
    @Override
    protected String getConnectionDescription() {
        return "redis://localhost:6379";
    }


    @Override
    protected String getDefaultPort() {
        return "6379";
    }

    @Override
    public String getId() {
        return "redis";
    }

    @Override
    public String getDescription() {
        return "RESP2/RESP3 Protocol";
    }

    @Override
    protected ProtocolSettings buildProtocolSettings() {
        return new ByteProtocolSettingsWithLogin();
    }
}
