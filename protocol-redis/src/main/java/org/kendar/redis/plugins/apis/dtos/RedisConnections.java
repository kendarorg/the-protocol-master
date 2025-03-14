package org.kendar.redis.plugins.apis.dtos;

import java.util.ArrayList;
import java.util.List;

public class RedisConnections {
    private List<RedisConnection> connections = new ArrayList<>();
    private String instanceId;

    public List<RedisConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<RedisConnection> connections) {
        this.connections = connections;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
