package org.kendar.amqp.v09.executor;

import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

public class AmqpProtoContext extends NetworkProtoContext {
    private short channel = 1;

    public AmqpProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public void disconnect(Object connection) {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (AmqpProxySocket) conn.getConnection();
        sock.close();
    }

    public short getChannel() {
        return ++channel;
    }
}
