package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.dtos.Mqtt5Property;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * https://www.emqx.com/en/blog/mqtt-5-0-control-packets-02-publish-puback
 */
public class Subscribe extends BaseMqttState {

    private byte[] payload;
    private short packetIdentifier;


    public Subscribe() {
        super();
        setFixedHeader(MqttFixedHeader.SUBSCRIBE);
    }

    public Subscribe(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.SUBSCRIBE);
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
            rb.writeVarBInteger(all.length);
            rb.write(all);
        }
        rb.write(getPayload());
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var publish = new Subscribe();
        publish.setFullFlag(event.getFullFlag());
        var context = (MqttContext) event.getContext();
        publish.setPacketIdentifier(bb.getShort());
        publish.setProtocolVersion(context.getProtocolVersion());
        //Variable header for MQTT >=5
        if (publish.isVersion(MqttProtocol.VERSION_5)) {
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

        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                publish,
                new SubscribeAck()
        ));
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public short getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }


}
