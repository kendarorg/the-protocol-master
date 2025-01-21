package org.kendar.http.api;

import java.util.List;

public class ProtocolIndex {
    private List<String> protocols;
    private String protocol;
    private String instanceId;

    public void setProtocols(List<String> protocols) {

        this.protocols = protocols;
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
