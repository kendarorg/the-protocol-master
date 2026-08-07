package org.kendar.amqp.v10.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySplitterState;

/**
 * Splits the broker-side byte stream into single AMQP 1.0 frames on the 4-byte
 * big-endian size prefix (size INCLUDES itself). Also lets the 8-byte protocol
 * header through as a single unit. Analog of the v09 {@code GenericFrame}.
 */
public class GenericFrame extends ProtoState implements NetworkReturnMessage, NetworkProxySplitterState {

    public GenericFrame() {
        super();
    }

    public GenericFrame(Class<?>... events) {
        super(events);
    }

    @Override
    public void write(BBuffer rb) {
        throw new TPMProtocolException("Not implemented");
    }

    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        // The relay loop resets position to -1 via truncate(); always start at 0.
        rb.setPosition(0);
        if (rb.size() < 8) {
            return false;
        }
        var head = rb.getBytes(0, 4);
        if (head[0] == 'A' && head[1] == 'M' && head[2] == 'Q' && head[3] == 'P') {
            return true; // 8-byte protocol header
        }
        rb.setPosition(0);
        var size = rb.getInt();
        rb.setPosition(0);
        return size >= 8 && rb.size() >= size;
    }

    public BytesEvent execute(BytesEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        var head = rb.getBytes(0, 4);
        int len;
        if (head[0] == 'A' && head[1] == 'M' && head[2] == 'Q' && head[3] == 'P') {
            len = 8;
        } else {
            rb.setPosition(0);
            len = rb.getInt();
        }
        rb.setPosition(0);
        var content = rb.getBytes(len); // advances position to len so truncate() drops exactly this message
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
