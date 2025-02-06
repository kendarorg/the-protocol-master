package org.kendar.events;

import java.util.Map;

public class ReportDataEvent implements TpmEvent {

    private String instanceId;
    private String protocol;
    private String query;
    private long connectionId;
    private long timestamp;
    private long duration;
    private Map<String, Object> tags;

    public ReportDataEvent() {
    }

    public ReportDataEvent(String instanceId,
                           String protocol,
                           String query,
                           long connectionId,
                           long timestamp,
                           long duration,
                           Map<String, Object> tags) {
        this.instanceId = instanceId;
        this.protocol = protocol;
        this.query = query;
        this.connectionId = connectionId;
        this.timestamp = timestamp;
        this.duration = duration;
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "ReportDataEvent{" +
                "instanceId='" + instanceId + '\'' +
                ", protocol='" + protocol + '\'' +
                ", query='" + query + '\'' +
                ", connectionId=" + connectionId +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                ", tags=" + tags +
                '}';
    }

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

    public Map<String, Object> getTags() {
        return tags;
    }

    public long getConnectionId() {
        return connectionId;
    }
}
