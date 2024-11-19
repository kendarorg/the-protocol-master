package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicRecordingPluginSettings extends PluginSettings {
    private boolean ignoreTrivialCalls = true;

    public boolean isIgnoreTrivialCalls() {
        return ignoreTrivialCalls;
    }

    public void setIgnoreTrivialCalls(boolean ignoreTrivialCalls) {
        this.ignoreTrivialCalls = ignoreTrivialCalls;
    }
}
