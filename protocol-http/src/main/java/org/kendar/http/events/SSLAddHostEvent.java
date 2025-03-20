package org.kendar.http.events;


import org.kendar.events.TpmEvent;

public class SSLAddHostEvent implements TpmEvent {
    private String host;
    private String instanceId;

    public SSLAddHostEvent() {
    }

    public SSLAddHostEvent(String host, String instanceId) {
        this.host = host;
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
