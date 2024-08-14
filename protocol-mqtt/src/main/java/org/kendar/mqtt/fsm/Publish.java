package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.dtos.Mqtt5Property;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * https://www.emqx.com/en/blog/mqtt-5-0-control-packets-02-publish-puback
 */
public class Publish extends BaseMqttState {

    private String topicName;
    private byte[] payload;
    private short packetIdentifier;

    public Publish() {
        super();
        setFixedHeader(MqttFixedHeader.PUBLISH);
    }

    public Publish(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBLISH);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        throw new RuntimeException("writeFrameContent");
    }

    @Override
    protected boolean canRunFrame(MqttPacket event) {
        return true;
    }


    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {
        var publish = new Publish();
        var context = (MqttContext) event.getContext();
        publish.setTopicName(bb.readUtf8String());
        publish.setPacketIdentifier(bb.getShort());
        //Variable header for MQTT >=5
        if(context.isVersion(MqttProtocol.VERSION_5)) {
            var propertiesLength = bb.readVarBInteger();
            if (propertiesLength.getValue() > 0) {
                publish.setProperties(new ArrayList<>());
                var start = bb.getPosition();
                var end = start + propertiesLength.getValue();
                while (bb.getPosition() < end) {
                    var propertyType = Mqtt5PropertyType.of(bb.get());
                    publish.getProperties().add(new Mqtt5Property(propertyType, bb));
                }
            }
        }
        publish.setPayload(bb.getRemaining());
        throw new RuntimeException("writeFrameContent");
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }

    public short getPacketIdentifier() {
        return packetIdentifier;
    }
}
