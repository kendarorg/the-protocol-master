package org.kendar.ui.dto;

public class PluginDto  extends BaseHtmxDto{
    private boolean active;

    public PluginDto() {
    }

    public PluginDto(String instanceId, String protocol, String id, boolean active) {
        this.instanceId = instanceId;
        this.protocol = protocol;
        this.id = id;
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private String instanceId;
    private String protocol;
    private String id;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
