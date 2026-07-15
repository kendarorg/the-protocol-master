package org.kendar.kafka.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.fsm.events.KafkaResponseEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

/**
 * Base for the state the proxy uses to read a broker response frame and relay it
 * to the client. It is simultaneously a {@link ProtoState} (matched on
 * correlation id) and a {@link NetworkReturnMessage} (writes the possibly
 * transformed payload back to the client), mirroring the AMQP frame pattern.
 * <p>
 * Default behaviour is byte-exact passthrough. Semantic subclasses override
 * {@link #transform(byte[], KafkaContext)} to decode → mutate (address rewrite /
 * version cap) → re-encode.
 */
public abstract class KafkaResponseState extends ProtoState implements NetworkReturnMessage {
    protected final int expectedCorrelationId;
    protected final short apiVersion;
    private byte[] payload;

    protected KafkaResponseState(int expectedCorrelationId, short apiVersion) {
        super(KafkaResponseEvent.class);
        this.expectedCorrelationId = expectedCorrelationId;
        this.apiVersion = apiVersion;
    }

    public boolean canRun(KafkaResponseEvent event) {
        return event.getCorrelationId() == expectedCorrelationId;
    }

    public Iterator<ProtoStep> execute(KafkaResponseEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        var raw = rb.getBytes(rb.size());
        var context = (KafkaContext) event.getContext();
        context.takeInFlight(event.getCorrelationId());

        byte[] out;
        try {
            out = transform(raw, context);
        } catch (RuntimeException ex) {
            // On any decode problem, fail safe to byte-exact passthrough rather
            // than corrupting the stream.
            out = raw;
        }

        var copy = newInstance();
        copy.payload = out;
        return iteratorOfList(copy);
    }

    @Override
    public void write(BBuffer rb) {
        rb.write(payload);
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    /** Factory returning a fresh instance of the concrete subclass (same class). */
    protected abstract KafkaResponseState newInstance();

    /** Transform the raw broker frame before relaying. Default: unchanged. */
    protected byte[] transform(byte[] raw, KafkaContext context) {
        return raw;
    }

    /** Public entry point to {@link #transform} for unit tests. */
    public byte[] rewrite(byte[] raw, KafkaContext context) {
        return transform(raw, context);
    }
}
