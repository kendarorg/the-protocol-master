package org.kendar.mqtt.fsm.dtos;

public class Topic {
    private String topic;
    private byte type;

    public Topic() {
    }

    public Topic(String topic, byte type) {
        this.topic = topic;
        this.type = type;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }
}
