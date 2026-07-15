package org.kendar.kafka.context;

import org.kendar.kafka.utils.KafkaProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-connection Kafka state. Holds the in-flight map (correlation id -&gt; the
 * request's api key/version/client id) so a response — which carries only a
 * correlation id on the wire — can be resolved back to its api key/version when
 * semantic decoding (rewrite / tagging) is required.
 */
public class KafkaContext extends NetworkProtoContext {

    /** correlation id -&gt; in-flight request metadata. */
    private final ConcurrentHashMap<Integer, InFlight> inFlight = new ConcurrentHashMap<>();

    public KafkaContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    public void registerInFlight(int correlationId, short apiKey, short apiVersion, String clientId) {
        inFlight.put(correlationId, new InFlight(apiKey, apiVersion, clientId));
    }

    /** Returns and removes the in-flight entry for a correlation id (null if unknown). */
    public InFlight takeInFlight(int correlationId) {
        return inFlight.remove(correlationId);
    }

    public InFlight peekInFlight(int correlationId) {
        return inFlight.get(correlationId);
    }

    @Override
    public void disconnect(Object connection) {
        super.disconnect(connection);
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        if (conn != null) {
            var sock = (KafkaProxySocket) conn.getConnection();
            if (sock != null) {
                sock.close();
            }
        }
    }

    @Override
    public boolean isConnected() {
        ProxyConnection conn = ((ProxyConnection) getValue("CONNECTION"));
        if (conn != null) {
            var sock = (KafkaProxySocket) conn.getConnection();
            if (sock != null && !sock.isConnected()) {
                return false;
            }
        }
        return super.isConnected();
    }

    public static final class InFlight {
        public final short apiKey;
        public final short apiVersion;
        public final String clientId;

        InFlight(short apiKey, short apiVersion, String clientId) {
            this.apiKey = apiKey;
            this.apiVersion = apiVersion;
            this.clientId = clientId;
        }
    }
}
