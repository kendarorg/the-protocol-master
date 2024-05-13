package org.kendar.redis;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;

public class Resp3Protocol extends NetworkProtoDescriptor {
    @Override
    public boolean isBe() {
        return false;
    }

    @Override
    public int getPort() {
        return 6379;
    }

    @Override
    protected void initializeProtocol() {

    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        return null;
    }
}
