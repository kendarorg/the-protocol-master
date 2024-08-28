package org.kendar.mqtt.fsm;

import org.kendar.mqtt.MqttProtocol;
import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.fsm.dtos.Mqtt5Property;
import org.kendar.mqtt.utils.MqttBBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePropertiesMqttState extends BaseMqttState {
    private List<Mqtt5Property> properties;
    public BasePropertiesMqttState() {
        super();
    }

    public BasePropertiesMqttState(Class<?>... events) {
        super(events);
    }


    public List<Mqtt5Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Mqtt5Property> properties) {
        this.properties = properties;
    }

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

    protected void readProperties(BasePropertiesMqttState baseMqttState, MqttBBuffer bb) {
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
}
