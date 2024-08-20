package org.kendar.mqtt.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class MqttPacket extends BaseEvent implements NetworkReturnMessage {
    private final MqttFixedHeader fixedHeader;
    private final MqttBBuffer buffer;

    public byte getFullFlag() {
        return fullFlag;
    }

    private final byte fullFlag;

    public MqttPacket(ProtoContext context, Class<?> prevState,
                      MqttFixedHeader fixedHeader,
                      MqttBBuffer buffer,
                      byte fullFlag) {
        super(context, prevState);
        this.fixedHeader = fixedHeader;
        this.buffer = buffer;
        this.fullFlag = fullFlag;
    }

    @Override
    public void write(BBuffer resultBuffer) {

    }

    public MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    public MqttBBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "MqttPacket{" +
                "fixedHeader=" + fixedHeader  +
                '}';
    }
}
