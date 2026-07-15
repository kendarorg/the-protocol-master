package org.kendar.amqp.v10.context;

import org.kendar.amqp.v10.utils.Amqp10ProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-connection state for AMQP 1.0. A connection multiplexes sessions
 * (channels) and, within each session, links (handles). Session ids and
 * handle→link correlation live here (the analog of the v09 channel set).
 */
public class Amqp10ProtoContext extends NetworkProtoContext {
    private final Set<Short> sessions = new HashSet<>();
    // handle -> link name / source address, populated on attach (M3 correlation)
    private final Map<Long, String> links = new ConcurrentHashMap<>();

    public Amqp10ProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    @Override
    public void disconnect(Object connection) {
        super.disconnect(connection);
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        if (conn != null) {
            var sock = (Amqp10ProxySocket) conn.getConnection();
            if (sock != null) {
                sock.close();
            }
        }
    }

    public Set<Short> getSessions() {
        return sessions;
    }

    public Map<Long, String> getLinks() {
        return links;
    }

    public boolean isConnected() {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        if (conn != null) {
            var sock = (Amqp10ProxySocket) conn.getConnection();
            if (sock != null && !sock.isConnected()) {
                return false;
            }
        }
        return super.isConnected();
    }
}
