package org.kendar.events;

import java.util.Map;
import java.util.Objects;

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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ReportDataEvent that = (ReportDataEvent) o;
        return connectionId == that.connectionId && timestamp == that.timestamp && duration == that.duration && Objects.equals(instanceId, that.instanceId) && Objects.equals(protocol, that.protocol) && Objects.equals(query, that.query) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, protocol, query, connectionId, timestamp, duration, tags);
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

    public String toStringTags(){
        return String.join(",",getTags().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
    }
}
