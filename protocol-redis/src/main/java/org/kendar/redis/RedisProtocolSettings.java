package org.kendar.redis;

import org.kendar.di.annotations.TpmService;
import org.kendar.settings.ByteProtocolSettings;

@TpmService(tags = "redis")
public class RedisProtocolSettings extends ByteProtocolSettings {
    public RedisProtocolSettings() {
        setProtocol("redis");
    }
}
