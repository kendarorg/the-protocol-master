package org.kendar.redis.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.Reps3Context;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.parser.Resp3Parser;

import java.util.Iterator;

public class Resp3PullState extends ProtoState implements NetworkReturnMessage {
    private Resp3Parser parser = new Resp3Parser();

    public Resp3Message getEvent() {
        return event;
    }

    private Resp3Message event;

    public Resp3PullState() {
        super();
    }

    private boolean proxy;
    public Resp3PullState asProxy() {
        this.proxy = true;
        return this;
    }

    public Resp3PullState(Class<?>... events) {
        super(events);
    }
    @Override
    public void write(BBuffer resultBuffer) {
        try {
            var bytes = event.getMessage().getBytes("ASCII");
            resultBuffer.write(bytes);
        }catch (Exception ex){
            System.err.println(ex.getMessage());
        }
    }

    public boolean canRun(Resp3Message event) {
        return true;
    }

    public Iterator<ProtoStep> execute(Resp3Message event) {
        var context = (Reps3Context) event.getContext();
        var proxy = (Resp3Proxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));
        if(!this.proxy) {
            return iteratorOfRunnable(() -> proxy.execute(context,
                    connection,
                    event,
                    new Resp3PullState().asProxy()
            ));
        }else{
            this.event = event;
            return iteratorOfList(event);
        }
    }
}
