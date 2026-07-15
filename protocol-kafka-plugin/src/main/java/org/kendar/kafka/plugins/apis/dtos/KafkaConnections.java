package org.kendar.kafka.plugins.apis.dtos;

import java.util.List;

/** JTE model: the publish panel's connection list. */
public class KafkaConnections {
    private List<KafkaConnection> connections;
    private String instanceId;

    public List<KafkaConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<KafkaConnection> connections) {
        this.connections = connections;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
