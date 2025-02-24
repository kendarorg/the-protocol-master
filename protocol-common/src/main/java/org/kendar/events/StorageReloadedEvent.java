package org.kendar.events;

public class StorageReloadedEvent implements TpmEvent {
    public String getSettings() {
        return settings;
    }

    private String settings;

    public TpmEvent withSettings(String settings) {
        this.settings = settings;
        return this;
    }
}
