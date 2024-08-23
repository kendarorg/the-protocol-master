package org.kendar.mqtt.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MqttPacketTranslator extends ProtoState implements NetworkReturnMessage, InterruptProtoState {
    private static final Logger log = LoggerFactory.getLogger(MqttPacketTranslator.class);
    private static final List<MqttFixedHeader> packetIdAwareMessages = new ArrayList<MqttFixedHeader>();
    private static int count = 0;
    private boolean proxy;

    public MqttPacketTranslator() {
        super();
    }

    public MqttPacketTranslator(Class<?>... events) {
        super(events);
    }

    public MqttPacketTranslator asProxy() {
        this.proxy = true;
        return this;
    }

    @Override
    public void write(BBuffer rb) {
        throw new RuntimeException();
    }

    public boolean canRun(BytesEvent event) {
        var rb = (MqttBBuffer) event.getBuffer();
        rb.setPosition(0);
        if (rb.size() < 1) {
            return false;
        }
        //First get the
        var byteValue = rb.get();
        var flag = MqttFixedHeader.of(byteValue);
        var varBValue = rb.readVarBInteger();
        if (rb.size() < (1 + varBValue.getLength() + varBValue.getValue())) {
            rb.setPosition(0);
            throw new AskMoreDataException();
        }
        rb.setPosition(0);
        return true;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        count++;
        var rb = (MqttBBuffer) event.getBuffer();
        var fullFlag = rb.get();
        var flag = MqttFixedHeader.of(fullFlag);

        var varBValue = rb.readVarBInteger();
        var data = rb.getBytes((int) varBValue.getValue());
        var bb = new MqttBBuffer(rb.getEndianness());
        bb.write(data);
        bb.setPosition(0);
        String packetIdentifier = null;
        switch (flag) {
            case PUBLISH:
                bb.readUtf8String();
                packetIdentifier = "P" + bb.getShort();
                bb.setPosition(0);
                break;
            case PUBCOMP:
            case PUBACK:
            case PUBREC:
            case PUBREL:
                packetIdentifier = "P" + bb.getShort();
                bb.setPosition(0);
                break;
            case SUBSCRIBE:
            case SUBACK:
                packetIdentifier = "S" + bb.getShort();
                bb.setPosition(0);
                break;
            case UNSUBSCRIBE:
            case UNSUBACK:
                packetIdentifier = "U" + bb.getShort();
                bb.setPosition(0);
                break;
        }


        if (!proxy) {
            log.trace("[CL>TP][  ]: Founded flag: {} with var length: {} packet:{}", flag, varBValue.getValue(), packetIdentifier);
            event.getContext().send(new MqttPacket(event.getContext(), event.getPrevState(), flag, bb, fullFlag, packetIdentifier));
            return iteratorOfEmpty();
        } else {
            if (packetIdentifier != null && packetIdentifier.equalsIgnoreCase("1")
                    && flag == MqttFixedHeader.PUBACK) {
                System.out.println();
            }
            log.trace("[TP<SR][  ]: Founded flag: {} with var length: {} packet:{}", flag, varBValue.getValue(), packetIdentifier);
            return iteratorOfList(new MqttPacket(event.getContext(), event.getPrevState(), flag, bb, fullFlag, packetIdentifier));
        }
    }
}
