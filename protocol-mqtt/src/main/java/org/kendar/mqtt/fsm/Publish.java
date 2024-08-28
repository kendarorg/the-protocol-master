package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Arrays;
import java.util.Iterator;

/**
 * https://www.emqx.com/en/blog/mqtt-5-0-control-packets-02-publish-puback
 */
public class Publish extends BasePropertiesMqttState {
    protected static final JsonMapper mapper = new JsonMapper();
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
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeUtf8String(getTopicName());
        rb.writeShort(getPacketIdentifier());
        writeProperties(rb);
        rb.write(getPayload());
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {
        var dupFlag = (event.getFullFlag() & (byte) 8) == (byte) 8;
        var retainFlag = (event.getFullFlag() & (byte) 1) == (byte) 1;
        var qos = event.getFullFlag() >> 1 & (byte) 3;

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
        readProperties(publish, bb);
        publish.setPayload(bb.getRemaining());

        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        if (isProxyed()) {
            var storage = proxy.getStorage();
            var res = "{\"type\":\"" + publish.getClass().getSimpleName() + "\",\"data\":" +
                    mapper.serialize(publish) + "}";
            storage.write(
                    context.getContextId(),
                    null
                    , mapper.toJsonNode(res)
                    , 0, "RESPONSE", "MQTT");
            return iteratorOfList(publish);
        }
        if (qos == 1) {
            return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                    connection,
                    publish,
                    new PublishAck()
            ));
        } else if (qos == 2) {
            return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                    connection,
                    publish,
                    new PublishRec()
            ));
        }
        return iteratorOfRunnable(() -> proxy.sendAndForget(context,
                connection,
                publish
        ));
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
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

    public boolean isDupFlag() {
        return dupFlag;
    }

    public void setDupFlag(boolean dupFlag) {
        this.dupFlag = dupFlag;
    }

    public boolean isRetainFlag() {
        return retainFlag;
    }

    public void setRetainFlag(boolean retainFlag) {
        this.retainFlag = retainFlag;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
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
