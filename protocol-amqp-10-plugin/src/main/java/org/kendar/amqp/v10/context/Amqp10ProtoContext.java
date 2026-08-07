package org.kendar.amqp.v10.context;

import org.kendar.amqp.v10.utils.Amqp10ProxySocket;
import org.kendar.amqp.v10.utils.ReceiverLink;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-connection state for AMQP 1.0. A connection multiplexes sessions
 * (channels) and, within each session, links (handles). Session ids and
 * handle→link correlation live here (the analog of the v09 channel set).
 */
public class Amqp10ProtoContext extends NetworkProtoContext {
    private final Set<Short> sessions = new HashSet<>();
    // handle -> link name / source address, populated on attach (M3 correlation)
    private final Map<Long, String> links = new ConcurrentHashMap<>();
    // link name -> consumer receiver link (broker sender attach); the publish
    // plugin injects transfers onto these. Analog of v09 BASIC_CONSUME_CH_*.
    private final Map<String, ReceiverLink> receiverLinks = new ConcurrentHashMap<>();
    // per-session (channel) next delivery-id to use for an injected transfer,
    // kept one past the highest delivery-id observed on the session.
    private final Map<Short, AtomicLong> nextDeliveryId = new ConcurrentHashMap<>();

    public Amqp10ProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    /** Records a consumer link so the publish plugin can inject deliveries to it. */
    public void putReceiverLink(ReceiverLink link) {
        link.setLastAccess(System.currentTimeMillis());
        receiverLinks.put(link.getName(), link);
    }

    public Map<String, ReceiverLink> getReceiverLinks() {
        return receiverLinks;
    }

    /** Keeps the per-session delivery-id counter one past the highest one seen. */
    public void observeDeliveryId(short channel, long deliveryId) {
        nextDeliveryId.computeIfAbsent(channel, c -> new AtomicLong(0))
                .updateAndGet(cur -> Math.max(cur, deliveryId + 1));
    }

    /** Returns the next delivery-id to use on the session and advances the counter. */
    public long nextDeliveryId(short channel) {
        return nextDeliveryId.computeIfAbsent(channel, c -> new AtomicLong(0)).getAndIncrement();
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
