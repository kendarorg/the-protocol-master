package org.kendar.amqp.v09.executor;

import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

import java.util.HashSet;
import java.util.Set;

public class AmqpProtoContext extends NetworkProtoContext {
    private short channel = 1;
    private Set<Short> channels = new HashSet<>();
    private int consumeId;

    public AmqpProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
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

    public Set<Short> getChannels() {
        return channels;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }

    public int getConsumeId() {
        return consumeId;
    }
}
