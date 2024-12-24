package org.kendar.mqtt.apis.dtos;

public class MqttConnection {
    private int qos =0;
    private String topic;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    private Integer id;

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getQos() {
        return qos;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
