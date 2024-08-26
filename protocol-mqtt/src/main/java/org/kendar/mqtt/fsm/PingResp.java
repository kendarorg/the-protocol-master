package org.kendar.mqtt.fsm;

import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.InterruptProtoState;

import java.util.Iterator;

public class PingResp extends BaseMqttState implements ReturnMessage, InterruptProtoState {

    private byte reasonCode;

    public PingResp(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PINGRESP);
    }

    public PingResp() {
        setFixedHeader(MqttFixedHeader.PINGRESP);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {
        var publishRel = new PingResp();
        publishRel.setFullFlag(event.getFullFlag());
        return iteratorOfList(publishRel);
    }

}
