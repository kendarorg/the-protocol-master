package org.kendar.redis;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.redis.fsm.Resp3MessageTranslator;
import org.kendar.redis.fsm.Resp3PullState;
import org.kendar.redis.fsm.Resp3Subscribe;
import org.kendar.redis.fsm.Resp3Unsubscribe;
import org.kendar.redis.fsm.events.Resp3Message;

public class Resp3Protocol extends NetworkProtoDescriptor {
    private static final int PORT = 6379;
    private int port = PORT;


    public Resp3Protocol(int port) {
        this.port = port;
    }


    @Override
    public boolean isBe() {
        return true;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    protected void initializeProtocol() {
        addInterruptState(new Resp3MessageTranslator(BytesEvent.class));
        initialize(
                new ProtoStateWhile(
                        new ProtoStateSwitchCase(
                                new ProtoStateSequence(
                                        new Resp3Subscribe(Resp3Message.class),
                                        new Resp3Unsubscribe(Resp3Message.class)),
                                new Resp3PullState(Resp3Message.class))));


    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor,
                                         int contextId) {
        return new Resp3Context(protoDescriptor, contextId);
    }
}
