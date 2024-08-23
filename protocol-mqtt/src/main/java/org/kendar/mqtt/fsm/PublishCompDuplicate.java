package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.dtos.Mqtt5Property;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

import java.util.ArrayList;
import java.util.Iterator;

public class PublishCompDuplicate extends BaseMqttState implements ReturnMessage {
    private short packetIdentifier;
    private byte reasonCode;

    public PublishCompDuplicate(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBCOMP);
    }

    public PublishCompDuplicate() {
        setFixedHeader(MqttFixedHeader.PUBCOMP);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeShort(getPacketIdentifier());
        if (isVersion(MqttProtocol.VERSION_5)) {
            rb.write(getReasonCode());
            var tempRb = new MqttBBuffer(rb.getEndianness());
            for (var pp : getProperties()) {
                pp.write(tempRb);
            }
            var all = tempRb.getAll();
            rb.write((byte) all.length);
            rb.write(all);
        }
    }

    @Override
    protected boolean canRunFrame(MqttPacket event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var context = (MqttContext) event.getContext();
        var publishRec = this;
        publishRec.setPacketIdentifier(bb.getShort());
        publishRec.setFullFlag(event.getFullFlag());

        publishRec.setProtocolVersion(context.getProtocolVersion());
        if (publishRec.isVersion(MqttProtocol.VERSION_5)) {
            publishRec.setReasonCode(bb.get());
            var propertiesLength = (int) bb.get();
            if (propertiesLength > 0) {
                publishRec.setProperties(new ArrayList<>());
                var start = bb.getPosition();
                var end = start + propertiesLength;
                while (bb.getPosition() < end) {
                    var propertyType = Mqtt5PropertyType.of(bb.get());
                    publishRec.getProperties().add(new Mqtt5Property(propertyType, bb));
                }
            }
        }
        return iteratorOfEmpty();
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
