package org.kendar.events;

public class StartWriteEvent implements TpmEvent {
    private final String instanceId;

    public StartWriteEvent(String instanceId) {

        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
