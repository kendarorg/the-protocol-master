package org.kendar.apis.dtos;

public class PluginIndex {
    private final String id;
    private final boolean active;

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
