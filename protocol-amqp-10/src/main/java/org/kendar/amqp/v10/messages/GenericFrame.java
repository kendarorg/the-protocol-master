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
        var pos = rb.getPosition();
        if (rb.size() - pos < 8) {
            return false;
        }
        var head = rb.getBytes(pos, 4);
        rb.setPosition(pos);
        if (head[0] == 'A' && head[1] == 'M' && head[2] == 'Q' && head[3] == 'P') {
            // 8-byte protocol header
            return rb.size() - pos >= 8;
        }
        var size = rb.getInt();
        rb.setPosition(pos);
        return size >= 8 && rb.size() - pos >= size;
    }

    public BytesEvent execute(BytesEvent event) {
        var rb = event.getBuffer();
        var pos = rb.getPosition();
        var head = rb.getBytes(pos, 4);
        rb.setPosition(pos);
        int len;
        if (head[0] == 'A' && head[1] == 'M' && head[2] == 'Q' && head[3] == 'P') {
            len = 8;
        } else {
            var size = rb.getInt();
            rb.setPosition(pos);
            len = size;
        }
        var content = rb.getBytes(len);
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
