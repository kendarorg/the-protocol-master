package org.kendar.redis;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.utils.Resp3ProxySocket;

public class Resp3Context extends NetworkProtoContext {
    public Resp3Context(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    @Override
    public void disconnect(Object connection) {

        super.disconnect(connection);
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (Resp3ProxySocket) conn.getConnection();
        if (sock != null) {
            sock.close();
        }

    }
}
