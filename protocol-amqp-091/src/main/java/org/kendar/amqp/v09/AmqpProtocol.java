package org.kendar.amqp.v09;

import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.ProtocolHeader;
import org.kendar.amqp.v09.messages.frames.HearthBeatFrame;
import org.kendar.amqp.v09.messages.methods.channel.ChannelClose;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionClose;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionStartOk;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionTuneOk;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.protocol.fsm.Start;
import org.kendar.server.Channel;

public class AmqpProtocol extends ProtoDescriptor {
    private static int PORT = 5672;
    private int port = PORT;
    private static final boolean IS_BIG_ENDIAN = true;
    @Override
    protected void initializeProtocol() {
        addIntertwinedState(new HearthBeatFrame());

        addState(new Start(),
                new ProtocolHeader(BytesEvent.class));
        addState(new ProtocolHeader(),
                new ConnectionStartOk(BytesEvent.class));
        addState(new ConnectionStartOk(),
                new ConnectionTuneOk(BytesEvent.class));
        addState(new ConnectionTuneOk(),
                new ConnectionOpen(BytesEvent.class),
                new ConnectionClose(BytesEvent.class));

        addState(new ConnectionOpen(),
                new ChannelOpen(BytesEvent.class));

        addState(new ChannelOpen(),
                new ChannelClose(BytesEvent.class));

        addState(new ChannelClose(),
                new ConnectionClose(BytesEvent.class));
    }

    public AmqpProtocol(){

    }

    public AmqpProtocol(int port){

        this.port = port;
    }

    @Override
    public boolean isBe() {
        return IS_BIG_ENDIAN;
    }

    @Override
    public int getPort() {
        return port;
    }



    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, Channel client) {
        var result = new AmqpProtoContext(this, client);
        return result;
    }
}
