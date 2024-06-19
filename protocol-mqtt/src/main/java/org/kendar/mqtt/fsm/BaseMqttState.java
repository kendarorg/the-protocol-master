package org.kendar.mqtt.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public abstract class BaseMqttState extends ProtoState implements NetworkReturnMessage {
    private MqttFixedHeader fixedHeader;
    private boolean proxyed;
    public boolean isProxyed() {
        return proxyed;
    }

    public BaseMqttState asProxy() {
        this.proxyed = true;
        return this;
    }

    public BaseMqttState() {
        super();
    }

    public BaseMqttState(Class<?>... events) {
        super(events);
    }

    @Override
    public void write(BBuffer rb) {
        var mqttRb = (MqttBBuffer) rb;
        mqttRb.write(getFixedHeader().asByte());

        var tmpMqttBuffer = new MqttBBuffer(rb.getEndianness());
        writeFrameContent(tmpMqttBuffer);
        tmpMqttBuffer.setPosition(0);
        mqttRb.writeVarBInteger(tmpMqttBuffer.size());
        mqttRb.write(tmpMqttBuffer.getAll());
    }

    protected abstract void writeFrameContent(MqttBBuffer rb);

    public boolean canRun(MqttPacket event) {
        return MqttFixedHeader.isFlagSet(
                event.getFixedHeader().getValue(),
                getFixedHeader());
    }

    protected abstract boolean canRunFrame(MqttPacket event);

    public Iterator<ProtoStep> execute(MqttPacket event) {
        return executeFrame(event.getFixedHeader(), event.getBuffer(),event);
    }

    protected abstract Iterator<ProtoStep> executeFrame(
            MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event);

    public MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    public void setFixedHeader(MqttFixedHeader fixedHeader) {
        this.fixedHeader = fixedHeader;
    }
}
