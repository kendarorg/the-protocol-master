package org.kendar.amqp.v10.plugins.apis.dtos;

import java.util.ArrayList;
import java.util.List;

/** JTE model: the receiver links available to the publish panel. */
public class Amqp10Connections {
    private List<Amqp10Connection> connections = new ArrayList<>();
    private String instanceId;

    public List<Amqp10Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Amqp10Connection> connections) {
        this.connections = connections;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
