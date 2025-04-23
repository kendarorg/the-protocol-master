package org.kendar.mqtt.fsm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.kendar.buffers.BBuffer;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public abstract class BaseMqttState extends ProtoState implements
        NetworkReturnMessage {
    @JsonIgnore
    protected int protocolVersion;
    private byte fullFlag;
    private MqttFixedHeader fixedHeader;
    @JsonIgnore
    private boolean proxied;


    public BaseMqttState() {
        super();
    }

    public BaseMqttState(Class<?>... events) {
        super(events);
    }

    public boolean isVersion(int expectedVersion) {
        return protocolVersion == expectedVersion;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public boolean isProxied() {
        return proxied;
    }

    public BaseMqttState asProxy() {
        this.proxied = true;
        return this;
    }

    @Override
    public void write(BBuffer rb) {
        var mqttRb = (MqttBBuffer) rb;
        mqttRb.write(getFullFlag());

        var tmpMqttBuffer = new MqttBBuffer(rb.getEndianness());
        writeFrameContent(tmpMqttBuffer);
        tmpMqttBuffer.setPosition(0);
        mqttRb.writeVarBInteger(tmpMqttBuffer.size());
        mqttRb.write(tmpMqttBuffer.getAll());
    }

    protected abstract void writeFrameContent(MqttBBuffer rb);

    public boolean canRun(MqttPacket event) {
        return event.getFixedHeader().getValue() == getFixedHeader().getValue();
    }

    public Iterator<ProtoStep> execute(MqttPacket event) {
        setFullFlag(event.getFullFlag());
        return executeFrame(event.getFixedHeader(), event.getBuffer(), event);
    }

    protected abstract Iterator<ProtoStep> executeFrame(
            MqttFixedHeader fixedHeader, MqttBBuffer rb, MqttPacket event);

    public MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    public void setFixedHeader(MqttFixedHeader fixedHeader) {
        this.fixedHeader = fixedHeader;
    }

    public byte getFullFlag() {
        return fullFlag;
    }

    public void setFullFlag(byte fullFlag) {
        this.fullFlag = fullFlag;
    }
}
