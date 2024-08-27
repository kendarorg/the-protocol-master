package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttContext;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.MqttProxy;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class PublishComp extends BaseMqttState implements ReturnMessage {
    private short packetIdentifier;
    private byte reasonCode;

    public PublishComp(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBCOMP);
    }

    public PublishComp() {
        setFixedHeader(MqttFixedHeader.PUBCOMP);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeShort(getPacketIdentifier());
        writeProperties(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {

        var context = (MqttContext) event.getContext();
        var publishRec = new PublishComp();
        publishRec.setPacketIdentifier(bb.getShort());
        publishRec.setFullFlag(event.getFullFlag());

        publishRec.setProtocolVersion(context.getProtocolVersion());
        if (publishRec.isVersion(MqttProtocol.VERSION_5)) {
            publishRec.setReasonCode(bb.get());
            readProperties(publishRec, bb);
        }
        if (isProxyed()) {
            var proxy = (MqttProxy) context.getProxy();
            var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

            return iteratorOfRunnable(() -> proxy.sendAndForget(context,
                    connection,
                    publishRec
            ));
        }
        return iteratorOfList(publishRec);
    }

    public short getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(short packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }

    public byte getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(byte reasonCode) {
        this.reasonCode = reasonCode;
    }
}
