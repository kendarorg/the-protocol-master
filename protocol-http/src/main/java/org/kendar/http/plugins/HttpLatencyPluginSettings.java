package org.kendar.http.plugins;

import org.kendar.settings.PluginSettings;

public class HttpLatencyPluginSettings extends PluginSettings {
    private int minMs=0;

    public int getMinMs() {
        return minMs;
    }

    public void setMinMs(int minMs) {
        this.minMs = minMs;
    }

    public int getMaxMs() {
        return maxMs;
    }

    public void setMaxMs(int maxMs) {
        this.maxMs = maxMs;
    }

    private int maxMs=0;
}
