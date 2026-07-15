package org.kendar.kafka.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySplitterState;

/**
 * Splits the broker-side byte stream into single Kafka frames on the 4-byte
 * big-endian {@code size} prefix (size <b>excludes</b> itself; total frame =
 * {@code 4 + size}). Analog of the v09/v10 {@code GenericFrame}.
 */
public class KafkaFrame extends ProtoState implements NetworkReturnMessage, NetworkProxySplitterState {

    public KafkaFrame() {
        super();
    }

    public KafkaFrame(Class<?>... events) {
        super(events);
    }

    @Override
    public void write(BBuffer rb) {
        throw new TPMProtocolException("Not implemented");
    }

    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        if (rb.size() < 4) {
            return false;
        }
        var size = rb.getInt();
        rb.setPosition(0);
        return size >= 0 && rb.size() >= size + 4;
    }

    public BytesEvent execute(BytesEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        var size = rb.getInt();
        rb.setPosition(0);
        var content = rb.getBytes(size + 4); // advances position so truncate() drops exactly this frame
        var bb = new BBuffer();
        bb.write(content);
        bb.setPosition(0);
        return new BytesEvent(null, null, bb);
    }

    @Override
    public BytesEvent split(BytesEvent input) {
        return execute(input);
    }
}
