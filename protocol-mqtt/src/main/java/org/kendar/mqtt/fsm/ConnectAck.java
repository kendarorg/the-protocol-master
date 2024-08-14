package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.ProxyConnection;

import java.util.ArrayList;
import java.util.Iterator;

public class ConnectAck extends BaseMqttState implements ReturnMessage {
    private boolean sessionSet;
    private byte connectReasonCode;

    public ConnectAck(){
        setFixedHeader(MqttFixedHeader.CONNACK);
    }

    public boolean isSessionSet() {
        return sessionSet;
    }

    public void setSessionSet(boolean sessionSet) {
        this.sessionSet = sessionSet;
    }

    public byte getConnectReasonCode() {
        return connectReasonCode;
    }

    public void setConnectReasonCode(byte connectReasonCode) {
        this.connectReasonCode = connectReasonCode;
    }



    @Override
    protected boolean canRunFrame(MqttPacket event) {
        return (MqttFixedHeader.isFlagSet(event.getFixedHeader().getValue(),MqttFixedHeader.CONNACK));
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        var context = (MqttContext) event.getContext();
        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var bb = event.getBuffer();
        //https://www.emqx.com/en/blog/mqtt-5-0-control-packets-01-connect-connack
        var connectAckFlag = bb.get(); //SessionPresent
        var sessionSet = (connectAckFlag & 0x01) == 0x01;
        //TODOMQTT 3.2.2.2 Connect Reason Code
        var connectReasonCode = bb.get();
        setProperties(new ArrayList<>());
        setSessionSet(sessionSet);
        setConnectReasonCode(connectReasonCode);
        //WAs it not varinteger

        return iteratorOfList(this);
    }


    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.write((byte) (sessionSet?0x01:0x00));
        rb.write((byte) (connectReasonCode));
    }
}
