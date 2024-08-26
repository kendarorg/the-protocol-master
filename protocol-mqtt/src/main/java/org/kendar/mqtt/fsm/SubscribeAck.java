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

public class SubscribeAck extends BaseMqttState implements ReturnMessage {
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
        if (isVersion(MqttProtocol.VERSION_5)) {
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
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var context = (MqttContext) event.getContext();
        var publishAck = this;
        publishAck.setPacketIdentifier(bb.getShort());
        publishAck.setFullFlag(event.getFullFlag());

        publishAck.setProtocolVersion(context.getProtocolVersion());
        if (publishAck.isVersion(MqttProtocol.VERSION_5)) {
            var propertiesLength = (int) bb.get();
            if (propertiesLength > 0) {
                publishAck.setProperties(new ArrayList<>());
                var start = bb.getPosition();
                var end = start + propertiesLength;
                while (bb.getPosition() < end) {
                    var propertyType = Mqtt5PropertyType.of(bb.get());
                    publishAck.getProperties().add(new Mqtt5Property(propertyType, bb));
                }
            }
        }
        return iteratorOfList(publishAck);
    }

    public short getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }
}
