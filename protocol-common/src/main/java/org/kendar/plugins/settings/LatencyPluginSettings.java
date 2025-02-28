package org.kendar.plugins.settings;

public class LatencyPluginSettings extends BasicPercentPluginSettings {
    private int minMs = 0;
    private int maxMs = 0;

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
}
