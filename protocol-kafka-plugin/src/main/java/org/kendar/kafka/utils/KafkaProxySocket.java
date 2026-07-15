package org.kendar.kafka.utils;

import org.kendar.buffers.BBuffer;
import org.kendar.kafka.fsm.KafkaFrame;
import org.kendar.kafka.fsm.events.KafkaResponseEvent;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NettyProxySocket;
import org.kendar.proxy.NetworkProxySplitterState;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;

/**
 * Broker-side socket. Kafka has <b>no server push</b> — every response is the
 * reply to an in-flight request — so {@link #availableStates()} is empty: frames
 * are split on the 4-byte size prefix and queued, then matched to the expecting
 * request's response state by correlation id inside {@code BaseProxySocket.read}
 * (protocol-kafka.md §4 / §6.5).
 */
public class KafkaProxySocket extends NettyProxySocket {

    public KafkaProxySocket(NetworkProtoContext context, InetSocketAddress inetSocketAddress,
                            AsynchronousChannelGroup group) {
        super(context, inetSocketAddress, group);
    }

    @Override
    protected NetworkProxySplitterState getStateToRetrieveOneSingleMessage() {
        return new KafkaFrame();
    }

    @Override
    protected List<ProtoState> availableStates() {
        return List.of();
    }

    @Override
    protected List<? extends ProtocolEvent> buildPossibleEvents(NetworkProtoContext context, BBuffer buffer) {
        buffer.setPosition(0);
        int correlationId = 0;
        if (buffer.size() >= 8) {
            buffer.getInt();               // size
            correlationId = buffer.getInt();
            buffer.setPosition(0);
        }
        return List.of(new KafkaResponseEvent(context, null, buffer, correlationId));
    }
}
