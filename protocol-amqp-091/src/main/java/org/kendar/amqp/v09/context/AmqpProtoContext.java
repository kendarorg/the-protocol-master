package org.kendar.amqp.v09.context;

import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

import java.util.HashSet;
import java.util.Set;

public class AmqpProtoContext extends NetworkProtoContext {
    private final Set<Short> channels = new HashSet<>();
    private short channel = 1;
    private int consumeId;

    public AmqpProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    @Override
    public void disconnect(Object connection) {
        super.disconnect(connection);
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (AmqpProxySocket) conn.getConnection();
        if (sock != null) {
            sock.close();
        }
    }

    public short getChannel() {
        return ++channel;
    }

    public Set<Short> getChannels() {
        return channels;
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }

    public boolean isConnected() {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        var sock = (AmqpProxySocket) conn.getConnection();
        if (sock != null) {
            if (!sock.isConnected()) {
                return false;
            }
        }
        if (!super.isConnected()) {
            return false;
        }
        return true;
    }
}
