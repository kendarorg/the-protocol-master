package org.kendar.redis.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.Resp3Context;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.parser.Resp3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class Resp3Subscribe extends ProtoState implements NetworkReturnMessage {
    private static final Logger log = LoggerFactory.getLogger(Resp3Subscribe.class);
    private final Resp3Parser parser = new Resp3Parser();
    private Resp3Message event;
    private boolean proxy;

    public Resp3Subscribe() {
        super();
    }

    public Resp3Subscribe(Class<?>... events) {
        super(events);
    }

    public Resp3Message getEvent() {
        return event;
    }

    public Resp3Subscribe asProxy() {
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
        if (event.getData() instanceof List) {
            return ((List<?>) event.getData()).get(0) != null && ((List<?>) event.getData()).get(0).toString().equalsIgnoreCase("subscribe");

        }
        return false;
    }

    public Iterator<ProtoStep> execute(Resp3Message event) {
        var context = (Resp3Context) event.getContext();
        var proxy = (Resp3Proxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        if (isProxyed()) {
            return iteratorOfEmpty();

        }
        try {
            var parsed = parser.parse(event.getMessage());
            if (List.class.isAssignableFrom(parsed.getClass())) {
                var list = (List<?>) parsed;
                if (list.size() >= 2) {
                    if (list.get(0).toString().equalsIgnoreCase("subscribe")) {
                        context.setValue("QUEUE", list.get(1).toString());
                    }
                }
            }
        } catch (Exception ex) {

        }

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                event,
                new Resp3Response()
        ));
    }
}
