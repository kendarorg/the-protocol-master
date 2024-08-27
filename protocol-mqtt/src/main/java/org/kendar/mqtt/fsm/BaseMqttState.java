package org.kendar.mqtt.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.enums.MqttFixedHeader;
import org.kendar.mqtt.fsm.dtos.Mqtt5Property;
import org.kendar.mqtt.fsm.events.MqttPacket;
import org.kendar.mqtt.utils.MqttBBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseMqttState extends ProtoState implements
        NetworkReturnMessage {
    private List<Mqtt5Property> properties;
    private byte fullFlag;
    private int protocolVersion;
    private MqttFixedHeader fixedHeader;
    private boolean proxyed;




    protected void writeProperties(MqttBBuffer rb) {
        if (protocolVersion == MqttProtocol.VERSION_5) {
            var rbProperties = new MqttBBuffer(rb.getEndianness());
            for (var prop : getProperties()) {
                prop.toBytes(rbProperties);
            }
            var allBytes = rbProperties.getAll();
            rb.writeVarBInteger(allBytes.length);
            rb.write(allBytes);
        }
    }

    protected void readProperties(BaseMqttState baseMqttState, MqttBBuffer bb) {
        if (protocolVersion == MqttProtocol.VERSION_5) {
            var propertiesLengthValue = bb.readVarBInteger().getValue();
            if (propertiesLengthValue > 0) {
                baseMqttState.setProperties(new ArrayList<>());
                var start = bb.getPosition();
                var end = start + propertiesLengthValue;
                while (bb.getPosition() < end) {
                    var propertyType = Mqtt5PropertyType.of(bb.get());
                    baseMqttState.getProperties().add(new Mqtt5Property(propertyType, bb));
                }
            }
        }
    }

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

    public List<Mqtt5Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Mqtt5Property> properties) {
        this.properties = properties;
    }

    public boolean isProxyed() {
        return proxyed;
    }

    public BaseMqttState asProxy() {
        this.proxyed = true;
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
