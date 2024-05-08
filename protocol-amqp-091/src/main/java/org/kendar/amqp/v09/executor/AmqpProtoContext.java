package org.kendar.amqp.v09.executor;

import org.kendar.amqp.v09.utils.ProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

public class AmqpProtoContext extends NetworkProtoContext {
    private short channel = 1;

    @Override
    public void disconnect(Object connection) {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (ProxySocket) conn.getConnection();
        sock.close();
    }

    public AmqpProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
    }

    public short getChannel() {
        return ++channel;
    }
}
