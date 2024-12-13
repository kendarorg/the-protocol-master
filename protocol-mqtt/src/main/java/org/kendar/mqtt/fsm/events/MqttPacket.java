package org.kendar.mqtt.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class MqttPacket extends ProtocolEvent implements NetworkReturnMessage {
    private final MqttFixedHeader fixedHeader;
    private final MqttBBuffer buffer;
    private final byte fullFlag;

    public MqttPacket(ProtoContext context, Class<?> prevState,
                      MqttFixedHeader fixedHeader,
                      MqttBBuffer buffer,
                      byte fullFlag,
                      String packetIdentifier) {
        super(context, prevState);
        if (packetIdentifier != null) {
            System.out.println("PACKET " + packetIdentifier + " " + this.getClass().getSimpleName());
            getTag().add(Tag.of("PACKET", packetIdentifier).get(0));
        }
        this.fixedHeader = fixedHeader;
        this.buffer = buffer;
        this.fullFlag = fullFlag;
    }

    public byte getFullFlag() {
        return fullFlag;
    }

    @Override
    public void write(BBuffer resultBuffer) {

    }

    public MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    public MqttBBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "MqttPacket{" +
                "fixedHeader=" + fixedHeader +
                '}';
    }
}
