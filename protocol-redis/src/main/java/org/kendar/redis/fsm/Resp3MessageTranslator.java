package org.kendar.redis.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySplitterState;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.parser.Resp3Input;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class Resp3MessageTranslator extends ProtoState implements NetworkReturnMessage, InterruptProtoState, NetworkProxySplitterState {
    private static final Logger log = LoggerFactory.getLogger(Resp3MessageTranslator.class);
    private static final JsonMapper mapper = new JsonMapper();
    private final Resp3Parser parser = new Resp3Parser();
    private boolean proxy;

    public Resp3MessageTranslator() {
        super();
    }

    public Resp3MessageTranslator(Class<?>... events) {
        super(events);
    }

    @Override
    public void write(BBuffer resultBuffer) {

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

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var rb = event.getBuffer();
        var oldPos = rb.getPosition();
        rb.setPosition(oldPos);
        var allBytes = rb.getRemaining();
        var str = new String(allBytes);
        var input = Resp3Input.of(str);
        Object result = null;
        try {
            result = parser.parse(input);
            rb.setPosition(oldPos + input.getIndex());
        } catch (Resp3ParseException ex) {
            if (ex.isMissingData()) {
                rb.setPosition(0);
                throw new AskMoreDataException();
            }
        }


        if (!this.proxy) {
            log.debug("[SERVER ][RX]: " + mapper.serialize(result));
            event.getContext().send(new Resp3Message(event.getContext(), event.getPrevState(), result, input.getPreString()));
            return iteratorOfEmpty();
        } else {
            return iteratorOfList(new Resp3Message(event.getContext(), event.getPrevState(), result, input.getPreString()));
        }

    }

    public Resp3MessageTranslator asProxy() {
        this.proxy = true;
        return this;
    }

    @Override
    public BytesEvent split(BytesEvent event) {
        var rb = event.getBuffer();
        var oldPos = rb.getPosition();
        rb.setPosition(oldPos);
        var allBytes = rb.getRemaining();
        var str = new String(allBytes);
        var input = Resp3Input.of(str);
        Object result = null;
        try {
            result = parser.parse(input);
            rb.setPosition(oldPos + input.getIndex());
            var bb = rb.getBytes(oldPos,input.getIndex());
            var buff = ((NetworkProtoContext)event.getContext()).buildBuffer();
            buff.write(bb);
            buff.setPosition(0);
            return new BytesEvent(event.getContext(),event.getPrevState(),buff);
        } catch (Resp3ParseException ex) {
            if (ex.isMissingData()) {
                rb.setPosition(0);
                throw new AskMoreDataException();
            }
        }
        throw new AskMoreDataException();
    }
}
