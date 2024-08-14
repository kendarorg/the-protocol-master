package org.kendar.mqtt.fsm;

import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

/**
 * https://www.emqx.com/en/blog/mqtt-5-0-control-packets-02-publish-puback
 */
public class Publish extends BaseMqttState{

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
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        throw new RuntimeException("writeFrameContent");
    }
}
