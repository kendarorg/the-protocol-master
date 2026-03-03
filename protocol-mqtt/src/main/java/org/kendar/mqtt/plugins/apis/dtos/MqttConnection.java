package org.kendar.mqtt.plugins.apis.dtos;

import java.util.Objects;

public class MqttConnection {
    private int qos = 0;
    private String topic;
    private Integer id;
    private long lastAccess;

    @Override
    public String toString() {
        return "{" +
                "qos=" + qos +
                ", topic='" + topic + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MqttConnection that = (MqttConnection) o;
        return qos == that.qos && Objects.equals(topic, that.topic) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qos, topic, id);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
}
