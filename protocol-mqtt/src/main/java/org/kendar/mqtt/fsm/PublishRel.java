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
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class PublishRel extends BasePropertiesMqttState implements ReturnMessage {
    protected static final JsonMapper mapper = new JsonMapper();
    private short packetIdentifier;
    private byte reasonCode;

    public PublishRel(Class<?>... events) {
        super(events);
        setFixedHeader(MqttFixedHeader.PUBREL);
    }

    public PublishRel() {
        setFixedHeader(MqttFixedHeader.PUBREL);
    }

    @Override
    protected void writeFrameContent(MqttBBuffer rb) {
        rb.writeShort(getPacketIdentifier());
        writeProperties(rb);
    }


    @Override
    protected Iterator<ProtoStep> executeFrame(MqttFixedHeader fixedHeader, MqttBBuffer bb, MqttPacket event) {
        var context = (MqttContext) event.getContext();
        var publishRel = new PublishRel();

        publishRel.setPacketIdentifier(bb.getShort());

        publishRel.setFullFlag(event.getFullFlag());

        publishRel.setProtocolVersion(context.getProtocolVersion());
        if (publishRel.isVersion(MqttProtocol.VERSION_5)) {
            publishRel.setReasonCode(bb.get());
            readProperties( publishRel, bb);
        }
        var proxy = (MqttProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        if(isProxyed()){
            return iteratorOfList(publishRel);
        }else {
            var finalPublishRel = publishRel;
            return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                    connection,
                    finalPublishRel,
                    new PublishComp()
            ));
        }
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
