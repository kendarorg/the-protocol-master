package org.kendar.mqtt.fsm;

import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

import java.util.Iterator;

public class PublishRel extends BaseMqttState implements ReturnMessage {
    public PublishRel(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBREL);
    }
    public PublishRel(){
        setFixedHeader(MqttFixedHeader.PUBREL);
    }
    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(MqttPacket event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        return null;
    }
}
