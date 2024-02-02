package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.IntertwinedProtoState;
import org.kendar.proxy.ProxyConnection;
import org.kendar.server.SocketChannel;

import java.util.Iterator;

public class HearthBeatFrame extends Frame implements IntertwinedProtoState {

    public HearthBeatFrame(){
        super();
        setType(FrameType.HEARTHBIT.asByte());
    }

    public HearthBeatFrame(Class<?> ...events){
        super(events);
        setType(FrameType.HEARTHBIT.asByte());
    }


    @Override
    protected void writeFrameContent(BBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(BytesEvent event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, BytesEvent event) {
        var hbFrame = new HearthBeatFrame();
        hbFrame.setChannel(channel);

        var context = (AmqpProtoContext)event.getContext();
        var connection = ((ProxyConnection)event.getContext().getValue("CONNECTION"));
        var sock = (SocketChannel)connection.getConnection();

        var heartBeatFrame = new HearthBeatFrame();
        heartBeatFrame.setChannel(channel);
        sock.write(heartBeatFrame,context.buildBuffer());
        sock.read(heartBeatFrame);

        return iteratorOfList(heartBeatFrame);
    }
}
