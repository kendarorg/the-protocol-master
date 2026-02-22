package org.kendar.mqtt;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.mqtt.fsm.*;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.protocol.states.special.Tagged;
import org.kendar.settings.GlobalSettings;

import java.util.List;

@TpmService(tags = "mqtt")
public class MqttProtocol extends NetworkProtoDescriptor {
    public static final int VERSION_5 = 5;
    private static final int PORT = 1883;
    public static int VERSION_3 = 3;
    private int port = PORT;

    @TpmConstructor
    public MqttProtocol(GlobalSettings ini, MqttProtocolSettings settings, MqttProxy proxy,
                        @TpmNamed(tags = "mqtt") List<BasePluginDescriptor> plugins) {
        super(ini, settings, proxy, plugins);
        this.port = settings.getPort();
        this.setTimeout(settings.getTimeoutSeconds());
    }


    public MqttProtocol(int port) {
        this.port = port;
        var pp = new MqttProtocolSettings();
        pp.setPort(port);
        setSettings(pp);
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
        addInterruptState(new MqttPacketTranslator(BytesEvent.class));
        addInterruptState(new Disconnect(MqttPacket.class));
        //addInterruptState(new Disconnect(PingReq.class));
        initialize(
                new ProtoStateSequence(
                        new Connect(MqttPacket.class),
                        new ProtoStateWhile(
                                new ProtoStateSwitchCase(
                                        new Tagged(
                                                Tag.ofKeys("PACKET"),
                                                new ProtoStateSwitchCase(
                                                        new ProtoStateSequence(
                                                                new Publish(MqttPacket.class),
                                                                new PublishRel(MqttPacket.class).asOptional()
                                                        ),
                                                        new Subscribe(MqttPacket.class),
                                                        new PublishAck(MqttPacket.class).asProxy(),
                                                        new ProtoStateSequence(
                                                                new PublishRec(MqttPacket.class).asProxy(),
                                                                new PublishComp(MqttPacket.class).asProxy()
                                                        )
                                                )
                                        )
                                        , new PingReq(MqttPacket.class)
                                )

                        )
                ));

    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor,
                                         int contextId) {
        return new MqttContext(protoDescriptor, contextId);
    }

    public BBuffer buildBuffer() {
        return new MqttBBuffer(isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }
}
