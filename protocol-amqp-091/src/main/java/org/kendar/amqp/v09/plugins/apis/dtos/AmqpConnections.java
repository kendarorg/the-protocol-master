package org.kendar.amqp.v09.plugins.apis.dtos;

import java.util.ArrayList;
import java.util.List;

public class AmqpConnections {
    private List<AmqpConnection> connections = new ArrayList<>();
    private String instanceId;

    public List<AmqpConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<AmqpConnection> connections) {
        this.connections = connections;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
