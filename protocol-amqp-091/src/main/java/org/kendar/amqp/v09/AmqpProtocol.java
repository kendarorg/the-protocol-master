package org.kendar.amqp.v09;

import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.ProtocolHeader;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.frames.HearthBeatFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicAck;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.amqp.v09.messages.methods.basic.BasicPublish;
import org.kendar.amqp.v09.messages.methods.channel.ChannelClose;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionClose;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionStartOk;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionTuneOk;
import org.kendar.amqp.v09.messages.methods.queue.QueueDeclare;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AmqpProtocol extends NetworkProtoDescriptor {

    private static final boolean IS_BIG_ENDIAN = true;
    public static ConcurrentHashMap<Integer, NetworkProtoContext> consumeContext;
    public static AtomicInteger consumeIdCounter;
    private static final int PORT = 5672;
    private int port = PORT;

    public AmqpProtocol() {

    }

    public AmqpProtocol(int port) {
        consumeContext = new ConcurrentHashMap<>();
        consumeIdCounter = new AtomicInteger(1);
        this.port = port;
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new HearthBeatFrame());

//        initialize(
//                new ProtoStateSequence(
//                        new ProtocolHeader(BytesEvent.class),
//                        new ConnectionStartOk(BytesEvent.class),
//                        new ConnectionTuneOk(BytesEvent.class),
//                        new ConnectionOpen(BytesEvent.class),
//                        new ProtoStateWhile(
//                                new ChannelOpen(BytesEvent.class),
//                                new ProtoStateSequence(
//                                        new QueueDeclare(BytesEvent.class),
//                                        new ProtoStateSwitchCase(
//                                                new ProtoStateWhile(
//                                                        new BasicPublish(BytesEvent.class),
//                                                        new HeaderFrame(BytesEvent.class),
//                                                        new ProtoStateWhile(
//                                                                new BodyFrame(BytesEvent.class)
//                                                        )
//                                                ),
//                                                new ProtoStateWhile(
//                                                        new BasicConsume(BytesEvent.class),
//                                                        new BasicAck(BytesEvent.class)
//                                                )
//                                        ).asOptional()
//                                ).asOptional(),
//                                new ChannelClose(BytesEvent.class)
//                        ).asOptional(),
//                        new ConnectionClose(BytesEvent.class)
//                )
//        );

        initialize(
                new ProtoStateSequence(
                        new ProtocolHeader(BytesEvent.class),
                        new ConnectionStartOk(BytesEvent.class),
                        new ConnectionTuneOk(BytesEvent.class),
                        new ConnectionOpen(BytesEvent.class),
                        new ProtoStateWhile(
                                new ProtoStateSwitchCase(
                                        new ChannelOpen(BytesEvent.class),
                                        new ChannelClose(BytesEvent.class),
                                        new QueueDeclare(BytesEvent.class),
                                        new BasicPublish(BytesEvent.class),
                                        new HeaderFrame(BytesEvent.class),
                                        new BodyFrame(BytesEvent.class),
                                        new BasicConsume(BytesEvent.class),
                                        new BasicAck(BytesEvent.class)
                                )
                        ),
                        new ConnectionClose(BytesEvent.class)
                )
        );
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
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        return new AmqpProtoContext(this);
    }
}
