package org.kendar.redis.utils;


import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.proxy.NetworkProxySplitterState;
import org.kendar.redis.fsm.GenericFrame;
import org.kendar.redis.fsm.Resp3MessageTranslator;
import org.kendar.redis.fsm.Resp3PullState;
import org.kendar.redis.fsm.events.Resp3Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;

public class Resp3ProxySocket extends NetworkProxySocket {
    private Resp3MessageTranslator translator= new Resp3MessageTranslator().asProxy();

    @Override
    protected NetworkProxySplitterState getStateToRetrieveOneSingleMessage() {
        return new GenericFrame();
    }

    @Override
    protected List<ProtoState> availableStates() {
        return List.of(new Resp3PullState().asProxy());
    }

    public Resp3ProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        super(context, inetSocketAddress, group);
    }


    @Override
    protected List<? extends BaseEvent> buildPossibleEvents(NetworkProtoContext context, BBuffer buffer) {
        var be = new BytesEvent(context,null,buffer);
        if(translator.canRunEvent(be)){
            var it = translator.execute(be);
            if(it.hasNext()) {
                var item = it.next().run();
                return List.of((Resp3Message)item);
            }
        }
        return List.of();
    }

    private static final Logger log = LoggerFactory.getLogger(Resp3ProxySocket.class.getName());

}
