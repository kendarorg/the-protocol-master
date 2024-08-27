package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

import java.util.Iterator;

public class PublishAck extends BaseMqttState implements ReturnMessage {
    private short packetIdentifier;
    private byte reasonCode;

    public PublishAck(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBACK);
    }

    public PublishAck() {
        setFixedHeader(MqttFixedHeader.PUBACK);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeShort(getPacketIdentifier());
        writeProperties(rb);
    }


    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var context = (MqttContext) event.getContext();
        var publishAck = new PublishAck();
        publishAck.setPacketIdentifier(bb.getShort());
        publishAck.setFullFlag(event.getFullFlag());

        publishAck.setProtocolVersion(context.getProtocolVersion());
        if (publishAck.isVersion(MqttProtocol.VERSION_5)) {
            publishAck.setReasonCode(bb.get());
            readProperties(publishAck, bb);
        }
        return iteratorOfList(publishAck);
    }

    public short getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }

    public byte getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(byte reasonCode) {
        this.reasonCode = reasonCode;
    }
}
