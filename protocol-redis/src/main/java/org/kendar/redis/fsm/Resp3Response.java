package org.kendar.redis.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.redis.fsm.events.Resp3Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Resp3Response extends ProtoState implements NetworkReturnMessage {
    private static final Logger log = LoggerFactory.getLogger(Resp3Response.class);
    private Resp3Message event;
    private boolean proxy;

    public Resp3Response() {
        super();
    }

    public Resp3Response(Class<?>... events) {
        super(events);
    }

    public Resp3Message getEvent() {
        return event;
    }

    public Resp3Response asProxy() {
        this.proxy = true;
        return this;
    }

    public boolean isProxyed() {
        return proxy;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        try {
            var bytes = event.getMessage().getBytes(StandardCharsets.US_ASCII);
            resultBuffer.write(bytes);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    public boolean canRun(Resp3Message event) {
        return true;
    }

    public Iterator<ProtoStep> execute(Resp3Message event) {
        this.event = event;
        return iteratorOfEmpty();
    }
}
