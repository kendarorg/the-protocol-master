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
import java.util.Arrays;
import java.util.Iterator;

/**
 * https://www.emqx.com/en/blog/mqtt-5-0-control-packets-02-publish-puback
 */
public class Publish extends BaseMqttState {

    private String topicName;
    private byte[] payload;
    private short packetIdentifier;
    private boolean dupFlag;
    private boolean retainFlag;
    private int qos;


    public Publish() {
        super();
        setFixedHeader(MqttFixedHeader.PUBLISH);
    }

    public Publish(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBLISH);
    }



    @Override
    protected boolean canRunFrame(MqttPacket event) {
        return true;
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeUtf8String(getTopicName());
        rb.writeShort(getPacketIdentifier());
        if(isVersion(MqttProtocol.VERSION_5)) {
            var tempRb = new MqttBBuffer(rb.getEndianness());
            for(var pp:getProperties()){
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
        var dupFlag = (event.getFullFlag() & (byte)8) == (byte)8;
        var retainFlag = (event.getFullFlag() & (byte)1) == (byte)1;
        var qos = event.getFullFlag()>>1 & (byte)3;

        var publish = new Publish();
        publish.setFullFlag(event.getFullFlag());
        publish.setDupFlag(dupFlag);
        publish.setRetainFlag(retainFlag);
        publish.setQos(qos);
        var context = (MqttContext) event.getContext();
        publish.setTopicName(bb.readUtf8String());
        publish.setPacketIdentifier(bb.getShort());
        publish.setProtocolVersion(context.getProtocolVersion());
        //Variable header for MQTT >=5
        if(publish.isVersion(MqttProtocol.VERSION_5)) {
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

        if (isProxyed()) {
            //TODOMQTT
            throw new RuntimeException("CANNOT HANDLE AS PROXY");
            //return iteratorOfEmpty();
        }
        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                publish,
                new PublishAck()
        ));
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

    public void setDupFlag(boolean dupFlag) {
        this.dupFlag = dupFlag;
    }

    public boolean isDupFlag() {
        return dupFlag;
    }

    public void setRetainFlag(boolean retainFlag) {
        this.retainFlag = retainFlag;
    }

    public boolean isRetainFlag() {
        return retainFlag;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getQos() {
        return qos;
    }

    @Override
    public String toString() {
        return "Publish{" +
                "topicName='" + topicName + '\'' +
                ", payload=" + Arrays.toString(payload) +
                ", packetIdentifier=" + packetIdentifier +
                ", dupFlag=" + dupFlag +
                ", retainFlag=" + retainFlag +
                ", qos=" + qos +
                '}';
    }
}
