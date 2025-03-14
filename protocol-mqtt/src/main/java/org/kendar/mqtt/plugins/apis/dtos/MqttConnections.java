package org.kendar.mqtt.plugins.apis.dtos;

import java.util.ArrayList;
import java.util.List;

public class MqttConnections {
    private List<MqttConnection> connections = new ArrayList<>();
    private String instanceId;

    public List<MqttConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<MqttConnection> connections) {
        this.connections = connections;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
