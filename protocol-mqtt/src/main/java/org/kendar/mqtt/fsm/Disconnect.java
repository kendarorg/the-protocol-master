package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.Stop;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class Disconnect extends BasePropertiesMqttState implements ReturnMessage, InterruptProtoState {

    private byte reasonCode;

    public Disconnect(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.DISCONNECT);
    }

    public Disconnect() {
        setFixedHeader(MqttFixedHeader.DISCONNECT);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {

        if (isVersion(MqttProtocol.VERSION_5)) {
            rb.write(getReasonCode());
            writeProperties(rb);
        }
    }
    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {
        var context = (MqttContext) event.getContext();
        var disconnect = new Disconnect();
        disconnect.setFullFlag(event.getFullFlag());

        disconnect.setProtocolVersion(context.getProtocolVersion());
        if (disconnect.isVersion(MqttProtocol.VERSION_5)) {
            disconnect.setReasonCode(bb.get());
            readProperties( disconnect, bb);
        }
        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        context.disconnect(connection);
        return iteratorOfRunner(() -> {
            proxy.sendAndForget(context,
                    connection,
                    disconnect
            );
            return new Stop();
        });
    }

    public byte getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(byte reasonCode) {
        this.reasonCode = reasonCode;
    }
}
