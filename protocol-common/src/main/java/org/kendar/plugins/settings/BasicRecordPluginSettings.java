package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicRecordPluginSettings extends PluginSettings {
    private boolean ignoreTrivialCalls = true;

    public boolean isIgnoreTrivialCalls() {
        return ignoreTrivialCalls;
    }

    public void setIgnoreTrivialCalls(boolean ignoreTrivialCalls) {
        this.ignoreTrivialCalls = ignoreTrivialCalls;
    }
}
