package org.kendar.events;

public class FinalizeWriteEvent implements TpmEvent {
    private final String instanceId;

    public FinalizeWriteEvent(String instanceId) {

        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
