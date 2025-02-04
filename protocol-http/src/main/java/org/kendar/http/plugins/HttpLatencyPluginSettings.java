package org.kendar.http.plugins;

import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpLatencyPluginSettings extends PluginSettings {
    private int minMs = 0;
    private int maxMs = 0;
    private List<String> latencySites = new ArrayList<>();

    public List<String> getLatencySites() {
        return latencySites;
    }

    public void setLatencySites(List<String> latencySites) {
        this.latencySites = latencySites;
    }

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
