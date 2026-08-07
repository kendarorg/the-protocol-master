package org.kendar.amqp.v10.utils;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.fsm.events.Amqp10Frame;
import org.kendar.amqp.v10.messages.EmptyFrame;
import org.kendar.amqp.v10.messages.GenericFrame;
import org.kendar.amqp.v10.messages.HeaderRelay;
import org.kendar.amqp.v10.messages.performatives.*;
import org.kendar.amqp.v10.messages.sasl.SaslMechanisms;
import org.kendar.amqp.v10.messages.sasl.SaslOutcome;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NettyProxySocket;
import org.kendar.proxy.NetworkProxySplitterState;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Broker-side socket. Splits the inbound stream into single frames
 * ({@link GenericFrame}) and handles the frames the broker can initiate as
 * {@code asProxy()} states (deliveries to consumers via {@code Transfer} being
 * the critical async path). Mirrors the v09 {@code AmqpProxySocket}.
 */
public class Amqp10ProxySocket extends NettyProxySocket {
    private final List<ProtoState> states = new ArrayList<>(Arrays.asList(
            // handshake frames the broker sends back, relayed to the client
            new HeaderRelay().asProxy(),
            new SaslMechanisms().asProxy(),
            new SaslOutcome().asProxy(),
            new Open().asProxy(),
            new Begin().asProxy(),
            // steady-state frames the broker can initiate
            new Transfer().asProxy(),
            new Flow().asProxy(),
            new Disposition().asProxy(),
            new Attach().asProxy(),
            new Detach().asProxy(),
            new End().asProxy(),
            new Close().asProxy(),
            new EmptyFrame().asProxy()));

    public Amqp10ProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        super(context, inetSocketAddress, group);
    }

    @Override
    protected NetworkProxySplitterState getStateToRetrieveOneSingleMessage() {
        return new GenericFrame();
    }

    @Override
    protected List<ProtoState> availableStates() {
        return states;
    }

    @Override
    protected List<? extends ProtocolEvent> buildPossibleEvents(NetworkProtoContext context, BBuffer buffer) {
        return List.of(new Amqp10Frame(context, null, buffer, (short) -1, frameTypeOf(buffer)));
    }

    private static byte frameTypeOf(BBuffer buffer) {
        if (buffer.size() >= 8) {
            var b = buffer.getBytes(0, 8);
            buffer.setPosition(0);
            var isHeader = b[0] == 'A' && b[1] == 'M' && b[2] == 'Q' && b[3] == 'P';
            if (!isHeader) {
                return b[5]; // frame type byte (AMQP=0 / SASL=1)
            }
        }
        return FrameType.AMQP.asByte();
    }
}
