package org.kendar.redis;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.utils.ProxySocket;

public class Reps3Context extends NetworkProtoContext {
    public Reps3Context(ProtoDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public void disconnect(Object connection) {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (ProxySocket) conn.getConnection();
        sock.close();
    }
}
