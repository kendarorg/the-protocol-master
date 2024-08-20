package org.kendar.mqtt;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.mqtt.fsm.*;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;

import java.util.concurrent.ConcurrentHashMap;

public class MqttProtocol extends NetworkProtoDescriptor {
    private static final int PORT = 1883;
    private int port = PORT;
    public static int VERSION_5=5;
    public static int VERSION_3=3;

    public static ConcurrentHashMap<Integer, NetworkProtoContext> consumeContext;

    private MqttProtocol() {
        consumeContext = new ConcurrentHashMap<>();
    }

    public MqttProtocol(int port) {this();this.port = port;}
    @Override
    public boolean isBe() {return true;}

    @Override
    public int getPort() {return port;}

    @Override
    protected void initializeProtocol() {
        addInterruptState(new MqttPacketTranslator(BytesEvent.class));
        initialize(
                new ProtoStateSequence(
                        new Connect(MqttPacket.class),
                        new ProtoStateWhile(
                                new ProtoStateSwitchCase(
                                        new Publish(MqttPacket.class),
                                        new ProtoStateWhile(
                                                new PublishRel(MqttPacket.class)
                                        )
                                )
                        )

                ));
    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        var result = new MqttContext(protoDescriptor);
        consumeContext.put(result.getContextId(), result);
        return result;
    }

    public BBuffer buildBuffer() {
        return new MqttBBuffer(isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }
}
