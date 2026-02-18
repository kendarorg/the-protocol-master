package org.kendar.events;

import org.kendar.protocol.context.NetworkProtoContext;

public class JdbcConnect implements TpmEvent{
    private final NetworkProtoContext protoContext;

    public JdbcConnect(NetworkProtoContext protoContext) {
        this.protoContext = protoContext;
    }

    public NetworkProtoContext getProtoContext() {
        return protoContext;
    }
}
