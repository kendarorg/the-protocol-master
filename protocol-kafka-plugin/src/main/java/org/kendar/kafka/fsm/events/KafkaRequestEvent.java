package org.kendar.kafka.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

/**
 * A single decoded Kafka request frame. The {@code buffer} holds the whole raw
 * frame (4-byte size prefix + request header + body) for byte-exact forwarding;
 * the header fields (api key/version, correlation id, client id) are parsed out
 * by {@link org.kendar.kafka.fsm.KafkaFrameTranslator} so request states can
 * route on {@code apiKey} without re-parsing.
 */
public class KafkaRequestEvent extends ProtocolEvent {
    private final BBuffer buffer;
    private final short apiKey;
    private final short apiVersion;
    private final int correlationId;
    private final String clientId;

    public KafkaRequestEvent(ProtoContext context, Class<?> prevState, BBuffer buffer,
                             short apiKey, short apiVersion, int correlationId, String clientId) {
        super(context, prevState);
        this.buffer = buffer;
        this.apiKey = apiKey;
        this.apiVersion = apiVersion;
        this.correlationId = correlationId;
        this.clientId = clientId;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    public short getApiKey() {
        return apiKey;
    }

    public short getApiVersion() {
        return apiVersion;
    }

    public int getCorrelationId() {
        return correlationId;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public String toString() {
        return "KafkaRequestEvent{apiKey=" + apiKey + ", apiVersion=" + apiVersion
                + ", correlationId=" + correlationId + '}';
    }
}
