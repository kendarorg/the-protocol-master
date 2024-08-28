package org.kendar.mqtt.fsm.dtos;

import org.kendar.mqtt.enums.Mqtt5PropertyType;
import org.kendar.mqtt.utils.MqttBBuffer;

public class Mqtt5Property {
    private Mqtt5PropertyType type;
    private byte[] byteValue;
    private long longValue;
    private String stringValue;

    public Mqtt5Property() {

    }

    public Mqtt5Property(Mqtt5PropertyType type, Object value) {

        this.type = type;
        switch (type.asByte()) {
            case (0x03):
            case (0x08):
            case (0x12):
            case (0x15):
            case (0x1A):
            case (0x1C):
            case (0x1F):
            case (0x26):
                if (value instanceof MqttBBuffer) {
                    stringValue = ((MqttBBuffer) value).readUtf8String();
                } else {
                    stringValue = (String) value;
                }
                break;
            case (0x09):
            case (0x16):
                if (value instanceof MqttBBuffer) {
                    var len = ((MqttBBuffer) value).getShort();
                    byteValue = ((MqttBBuffer) value).getBytes(len);
                } else {
                    byteValue = (byte[]) value;
                }

                break;
            case (0x0B):
                if (value instanceof MqttBBuffer) {
                    var res = ((MqttBBuffer) value).readVarBInteger();
                    longValue = res.getValue();
                } else {
                    longValue = (int) value;
                }
                break;
            case (0x02):
            case (0x11):
            case (0x18):
            case (0x27):
                if (value instanceof MqttBBuffer) {
                    longValue = ((MqttBBuffer) value).getInt();
                } else {
                    longValue = (int) value;
                }
                break;
            case (0x13):
            case (0x21):
            case (0x22):
            case (0x23):
                if (value instanceof MqttBBuffer) {
                    longValue = ((MqttBBuffer) value).getShort();
                } else {
                    longValue = (int) value;
                }
                break;
            default:
                if (value instanceof MqttBBuffer) {
                    longValue = ((MqttBBuffer) value).get();
                } else {
                    longValue = (byte) value;
                }
                break;
        }
    }

    public void write(MqttBBuffer value) {
        value.write(type.asByte());
        switch (type.asByte()) {
            case (0x03):
            case (0x08):
            case (0x12):
            case (0x15):
            case (0x1A):
            case (0x1C):
            case (0x1F):
            case (0x26):
                value.writeUtf8String(stringValue);
                break;
            case (0x09):
            case (0x16):
                value.writeShort((short) byteValue.length);
                value.write(byteValue);
                break;
            case (0x0B):
                value.writeVarBInteger(longValue);
                break;
            case (0x02):
            case (0x11):
            case (0x18):
            case (0x27):
                value.writeInt((int) longValue);
                break;
            case (0x13):
            case (0x21):
            case (0x22):
            case (0x23):
                value.writeShort((short) longValue);
                break;
            default:
                value.write((byte) longValue);
                break;
        }
    }

    public Mqtt5PropertyType getType() {
        return type;
    }

    public void setType(Mqtt5PropertyType type) {
        this.type = type;
    }

    public byte[] getByteValue() {
        return byteValue;
    }

    public void setByteValue(byte[] byteValue) {
        this.byteValue = byteValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public void toBytes(MqttBBuffer bb) {
        bb.write(type.asByte());
        switch (type.asByte()) {
            case (0x03):
            case (0x08):
            case (0x12):
            case (0x15):
            case (0x1A):
            case (0x1C):
            case (0x1F):
            case (0x26):
                bb.writeUtf8String(stringValue);
                break;
            case (0x09):
            case (0x16):
                bb.writeShort((short) byteValue.length);
                bb.write(byteValue);
                break;
            case (0x0B):
                bb.writeVarBInteger(longValue);
                break;
            case (0x02):
            case (0x11):
            case (0x18):
            case (0x27):
                bb.writeInt((int) longValue);
                break;
            case (0x13):
            case (0x21):
            case (0x22):
            case (0x23):
                bb.writeShort((short) longValue);
                break;
            default:
                bb.write((byte) longValue);
                break;
        }

    }
}
