package org.kendar.amqp.v09.utils;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.GenericFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.*;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionBlocked;
import org.kendar.amqp.v09.messages.methods.connection.ConnectionUnblocked;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.proxy.NetworkProxySplitterState;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AmqpProxySocket extends NetworkProxySocket {
    public AmqpProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        super(context, inetSocketAddress, group);
    }

    private final List<ProtoState> states = new ArrayList<>(Arrays.asList(
            new BasicDeliver(),
            new BasicCancel().asProxy(),
            new HeaderFrame().asProxy(),
            new BodyFrame().asProxy(),
            new ConnectionBlocked().asProxy(),
            new ConnectionUnblocked().asProxy(),
            new BasicAck().asProxy(),
            new BasicNack().asProxy(),
            new BasicReturn().asProxy(),
            new BasicGetEmpty().asProxy()));

    @Override
    protected NetworkProxySplitterState getStateToRetrieveOneSingleMessage() {
        return new GenericFrame();
    }

    @Override
    protected List<ProtoState> availableStates() {
        return states;
    }

    @Override
    protected List<? extends BaseEvent> buildPossibleEvents(NetworkProtoContext context, BBuffer buffer) {
        return List.of(new AmqpFrame(context, null, buffer, (short) -1));
    }
}
