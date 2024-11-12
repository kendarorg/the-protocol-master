package org.kendar.apis.dtos;

public class FilterIndex {
    private String id;
    private boolean active;

    public FilterIndex(String id, boolean active) {

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
