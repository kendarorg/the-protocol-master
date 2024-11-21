package org.kendar.apis.dtos;

public class PluginIndex {
    private String id;
    private boolean active;

    public PluginIndex() {

    }

    public PluginIndex(String id, boolean active) {

        this.id = id;
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public String getId() {
        return id;
    }
}
