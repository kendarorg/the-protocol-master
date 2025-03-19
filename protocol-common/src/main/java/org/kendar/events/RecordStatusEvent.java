package org.kendar.events;

public class RecordStatusEvent implements TpmEvent {
    private final boolean recording;
    private final String protocol;
    private final String filter;
    private final String instanceId;

    public RecordStatusEvent(boolean replaying, String protocol, String filter, String instanceId) {

        this.recording = replaying;
        this.protocol = protocol;
        this.filter = filter;
        this.instanceId = instanceId;
    }

    public boolean isRecording() {
        return recording;
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
