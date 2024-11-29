package org.kendar.events;

public class StartPlayEvent implements TpmEvent {
    private final String instanceId;

    public StartPlayEvent(String instanceId) {

        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
