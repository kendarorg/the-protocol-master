package org.kendar.events;

public class StorageReloadedEvent implements TpmEvent {
    private String settings;

    public String getSettings() {
        return settings;
    }

    public TpmEvent withSettings(String settings) {
        this.settings = settings;
        return this;
    }
}
