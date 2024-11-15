package org.kendar.events;

public class RecordStatusEvent implements TpmEvent {
    private final boolean replaying;
    private final String protocol;
    private final String filter;
    private final String instanceId;

    public RecordStatusEvent(boolean replaying, String protocol, String filter, String instanceId) {

        this.replaying = replaying;
        this.protocol = protocol;
        this.filter = filter;
        this.instanceId = instanceId;
    }

    public boolean isReplaying() {
        return replaying;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getFilter() {
        return filter;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
