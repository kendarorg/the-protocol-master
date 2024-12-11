package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.dtos.Topic;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * https://www.emqx.com/en/blog/mqtt-5-0-control-packets-02-publish-puback
 */
public class Subscribe extends BasePropertiesMqttState {

    private final List<Topic> topics = new ArrayList<>();
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
        writeProperties(rb);
        for (var topic : getTopics()) {
            rb.writeUtf8String(topic.getTopic());
            rb.write(topic.getType());
        }
    }

    public List<Topic> getTopics() {
        return topics;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var publish = new Subscribe();
        publish.setFullFlag(event.getFullFlag());
        var context = (MqttContext) event.getContext();
        publish.setPacketIdentifier(bb.getShort());
        publish.setProtocolVersion(context.getProtocolVersion());
        //Variable header for MQTT >=5
        readProperties(publish, bb);
        var subscriptions = (HashSet<String>) context.getValue("TOPICS");
        if (subscriptions == null) {
            subscriptions = new HashSet<>();
            context.setValue("TOPICS", subscriptions);
        }

        var payload = bb.getBytes(bb.getPosition(), bb.size() - bb.getPosition());
        while (bb.getPosition() < bb.size()) {
            var topic = bb.readUtf8String();
            var options = bb.get();
            subscriptions.add(options + "|" + topic);
            publish.getTopics().add(new Topic(topic, options));
        }

        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                publish,
                new SubscribeAck()
        ));
    }


    public short getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }


}
