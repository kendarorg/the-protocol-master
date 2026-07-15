package org.kendar.kafka.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

/**
 * A single Kafka response frame read from the broker. Responses carry only a
 * correlation id (no api key), so the api key/version are resolved from the
 * context's in-flight map when semantic decoding is needed; matching a response
 * to its expecting state is done purely on {@code correlationId}
 * (pipelining-safe, protocol-kafka.md §6.3).
 */
public class KafkaResponseEvent extends ProtocolEvent {
    private final BBuffer buffer;
    private final int correlationId;

    public KafkaResponseEvent(ProtoContext context, Class<?> prevState, BBuffer buffer, int correlationId) {
        super(context, prevState);
        this.buffer = buffer;
        this.correlationId = correlationId;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    public int getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return "KafkaResponseEvent{correlationId=" + correlationId + '}';
    }
}
