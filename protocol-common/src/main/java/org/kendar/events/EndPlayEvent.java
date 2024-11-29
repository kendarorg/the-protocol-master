package org.kendar.events;

public class EndPlayEvent implements TpmEvent {
    private final String instanceId;

    public EndPlayEvent(String instanceId) {

        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
