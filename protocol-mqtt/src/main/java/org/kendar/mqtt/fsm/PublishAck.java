package org.kendar.mqtt.fsm;

import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

import java.util.Iterator;

public class PublishAck  extends BaseMqttState implements ReturnMessage {
    public PublishAck(){
        setFixedHeader(MqttFixedHeader.PUBACK);
    }
    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(MqttPacket event) {
        return false;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        return null;
    }
}
