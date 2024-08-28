package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

import java.util.Iterator;

public class SubscribeAck extends BasePropertiesMqttState implements ReturnMessage {
    private short packetIdentifier;

    public SubscribeAck(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.SUBACK);
    }

    public SubscribeAck() {
        setFixedHeader(MqttFixedHeader.SUBACK);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeShort(getPacketIdentifier());
        writeProperties(rb);
    }


    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var context = (MqttContext) event.getContext();
        var subscribeAck = new SubscribeAck();
        subscribeAck.setPacketIdentifier(bb.getShort());
        subscribeAck.setFullFlag(event.getFullFlag());

        subscribeAck.setProtocolVersion(context.getProtocolVersion());
        readProperties(subscribeAck, bb);
        return iteratorOfList(subscribeAck);
    }

    public short getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }
}
