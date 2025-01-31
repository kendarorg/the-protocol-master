package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

import static org.kendar.protocol.descriptor.ProtoDescriptor.getNow;

public class HearthBeatFrame extends Frame implements InterruptProtoState {

    public HearthBeatFrame() {
        super();
        setType(FrameType.HEARTBEAT.asByte());
    }

    public HearthBeatFrame(Class<?>... events) {
        super(events);
        setType(FrameType.HEARTBEAT.asByte());
    }


    @Override
    protected void writeFrameContent(BBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(AmqpFrame event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, AmqpFrame event, int size) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var hbFrame = new HearthBeatFrame();
        hbFrame.setChannel((short) 0);

        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));
        var sock = (AmqpProxySocket) connection.getConnection();

        var heartBeatFrame = new HearthBeatFrame();
        heartBeatFrame.setChannel((short)0);
        if (isProxyed()) {
            heartBeatFrame.asProxy();
            //System.out.println("HEARTH BEAT PROXY");
            return iteratorOfList(heartBeatFrame);
        }
        context.setValue("HEARTBEAT_LAST",getNow());
        System.out.println("HEARTH BEAT STD");
        return iteratorOfRunnable(() -> proxy.sendAndForget(context,
                connection,
                heartBeatFrame
        ));
    }
}
