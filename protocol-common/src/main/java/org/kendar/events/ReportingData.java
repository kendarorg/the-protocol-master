package org.kendar.events;

import java.util.Map;

public class ReportingData implements TpmEvent {

    private final String instanceId;
    private final String protocol;
    private final String query;
    private final long connectionId;
    private final long timestamp;
    private final long duration;
    private final Map<String, String> tags;

    public String getInstanceId() {
        return instanceId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getQuery() {
        return query;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public ReportingData(String instanceId,
                         String protocol,
                         String query,
                         long connectionId,
                         long timestamp,
                         long duration,
                         Map<String,String> tags) {
        this.instanceId = instanceId;
        this.protocol = protocol;
        this.query = query;
        this.connectionId = connectionId;
        this.timestamp = timestamp;
        this.duration = duration;
        this.tags = tags;
    }
}
