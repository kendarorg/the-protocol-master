package org.kendar.http.plugins.settings;

import org.kendar.plugins.settings.BasicPercentPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpLatencyPluginSettings extends BasicPercentPluginSettings {
    private int minMs = 0;
    private int maxMs = 0;


    private List<String> target = new ArrayList<>();

    public List<String> getTarget() {
        return target;
    }

    public void setTarget(List<String> target) {
        this.target = target;
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
