package org.kendar.redis.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySplitterState;
import org.kendar.redis.parser.Resp3Input;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;

public class GenericFrame extends ProtoState implements NetworkReturnMessage, NetworkProxySplitterState {
    private final Resp3Parser parser = new Resp3Parser();

    @Override
    public void write(BBuffer resultBuffer) {
        throw new RuntimeException();
    }

    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        var oldPos = rb.getPosition();
        rb.setPosition(0);
        var allBytes = rb.getAll();
        if (allBytes.length == 0) {
            return false;
        }
        var str = new String(allBytes);
        var input = Resp3Input.of(str);
        try {
            Object result = parser.parse(input);
            rb.setPosition(0);
        } catch (Resp3ParseException ex) {
            if (ex.isMissingData()) {
                rb.setPosition(0);
                throw new AskMoreDataException();
            }
        }
        return true;
    }

    public BytesEvent execute(BytesEvent event) {
        var rb = event.getBuffer();


        var allBytes = rb.getAll();
        var str = new String(allBytes);
        var input = Resp3Input.of(str);
        Object result = null;
        try {
            result = parser.parse(input);
            var content = rb.getBytes(input.getIndex());
            rb.setPosition(input.getIndex());
            var bb = new BBuffer();
            bb.write(content);
            bb.setPosition(0);
            return new BytesEvent(null, null, bb);
        } catch (Resp3ParseException ex) {
            if (ex.isMissingData()) {
                rb.setPosition(0);
                throw new AskMoreDataException();
            }
        }

        throw new AskMoreDataException();
    }

    @Override
    public BytesEvent split(BytesEvent input) {
        return execute(input);
    }
}
