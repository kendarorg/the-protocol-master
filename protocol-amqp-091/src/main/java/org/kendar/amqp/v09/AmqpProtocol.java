package org.kendar.amqp.v09;

import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.AmqpFrameTranslator;
import org.kendar.amqp.v09.fsm.ProtocolHeader;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.frames.HearthBeatFrame;
import org.kendar.amqp.v09.messages.methods.basic.*;
import org.kendar.amqp.v09.messages.methods.channel.ChannelClose;
import org.kendar.amqp.v09.messages.methods.channel.ChannelOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionClose;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionOpen;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionStartOk;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionTuneOk;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeBind;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeDeclare;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeDelete;
import org.kendar.amqp.v09.messages.methods.exchange.ExchangeUnbind;
import org.kendar.amqp.v09.messages.methods.queue.*;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.protocol.states.special.Tagged;

import java.util.concurrent.ConcurrentHashMap;

public class AmqpProtocol extends NetworkProtoDescriptor {

    private static final boolean IS_BIG_ENDIAN = true;
    private static final int PORT = 5672;
    public static ConcurrentHashMap<Integer, NetworkProtoContext> consumeContext;

    private int port = PORT;

    private AmqpProtocol() {
        consumeContext = new ConcurrentHashMap<>();

    }

    public AmqpProtocol(int port) {
        this();
        this.port = port;
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new HearthBeatFrame());
        addInterruptState(new AmqpFrameTranslator(BytesEvent.class));

        initialize(
                new ProtoStateSequence(
                        new ProtocolHeader(BytesEvent.class),
                        new ConnectionStartOk(AmqpFrame.class),
                        new ConnectionTuneOk(AmqpFrame.class),
                        new ConnectionOpen(AmqpFrame.class),
                        new Tagged(
                                Tag.ofKeys("CHANNEL"),
                                new ProtoStateSequence(
                                        new ChannelOpen(AmqpFrame.class),
                                        new ProtoStateWhile(
                                                new ProtoStateSwitchCase(
                                                        new QueueDeclare(AmqpFrame.class),
                                                        new QueueBind(AmqpFrame.class),
                                                        new QueueUnbind(AmqpFrame.class),
                                                        new QueuePurge(AmqpFrame.class),
                                                        new QueueDelete(AmqpFrame.class),
                                                        new ExchangeDeclare(AmqpFrame.class),
                                                        new ExchangeBind(AmqpFrame.class),
                                                        new ExchangeUnbind(AmqpFrame.class),
                                                        new ExchangeDelete(AmqpFrame.class),
                                                        new BasicConsume(AmqpFrame.class),
                                                        new BasicCancel(AmqpFrame.class),
                                                        new BasicGet(AmqpFrame.class),
                                                        new ProtoStateSequence(
                                                                new BasicPublish(AmqpFrame.class),
                                                                new HeaderFrame(AmqpFrame.class),
                                                                new BodyFrame(AmqpFrame.class)
                                                        ),
                                                        new BasicAck(AmqpFrame.class),
                                                        new BasicNack(AmqpFrame.class),
                                                        new Reject(AmqpFrame.class)
                                                )
                                        ),
                                        new ChannelClose(AmqpFrame.class)
                                )
                        ),
                        new ConnectionClose(AmqpFrame.class)
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
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId) {
        return new AmqpProtoContext(this, contextId);
    }
}
