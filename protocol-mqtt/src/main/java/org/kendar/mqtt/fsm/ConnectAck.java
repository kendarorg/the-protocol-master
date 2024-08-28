package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

import java.util.Iterator;

public class ConnectAck extends BasePropertiesMqttState implements ReturnMessage {
    private boolean sessionSet;
    private byte connectReasonCode;

    public ConnectAck() {
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
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event) {
        var bb = event.getBuffer();
        var connect = new ConnectAck();
        //https://www.emqx.com/en/blog/mqtt-5-0-control-packets-01-connect-connack
        var connectAckFlag = bb.get(); //SessionPresent
        var sessionSet = (connectAckFlag & 0x01) == 0x01;
        //TODOMQTT 3.2.2.2 Connect Reason Code
        var connectReasonCode = bb.get();
        readProperties(connect, bb);
        connect.setSessionSet(sessionSet);
        connect.setConnectReasonCode(connectReasonCode);
        connect.setProtocolVersion(((MqttContext) event.getContext()).getProtocolVersion());
        connect.setFullFlag(event.getFullFlag());
        //WAs it not varinteger

        return iteratorOfList(connect);
    }


    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.write((byte) (sessionSet ? 0x01 : 0x00));
        rb.write((byte) (connectReasonCode));
        writeProperties(rb);
    }
}
