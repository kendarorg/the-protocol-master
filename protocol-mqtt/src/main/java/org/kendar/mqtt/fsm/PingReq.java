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

public class PingReq extends BaseMqttState implements ReturnMessage, InterruptProtoState {

    public PingReq(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PINGREQ);
    }

    public PingReq() {
        setFixedHeader(MqttFixedHeader.PINGREQ);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {
        var context = (MqttContext) event.getContext();
        var pingReq = new PingReq();

        pingReq.setFullFlag(event.getFullFlag());

        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        return iteratorOfRunner(() ->
            proxy.sendAndExpect(context,
                    connection,
                    pingReq,
                    new PingResp()
            )
        );
    }
}
