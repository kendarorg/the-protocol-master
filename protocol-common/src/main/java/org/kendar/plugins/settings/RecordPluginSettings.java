package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class RecordPluginSettings extends PluginSettings {
    private boolean record;

    public boolean isRecord() {
        return record;
    }

    public void setRecord(boolean record) {
        this.record = record;
    }
}
