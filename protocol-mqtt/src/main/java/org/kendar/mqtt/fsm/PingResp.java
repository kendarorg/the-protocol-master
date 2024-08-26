package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.proxy.ProxyConnection;

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
        var context = (MqttContext) event.getContext();
        var publishRel = new PingResp();
        //System.out.println(bb.toHexStringUpToLength(0,10));


        publishRel.setFullFlag(event.getFullFlag());


        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        return iteratorOfList(publishRel);
    }

}
