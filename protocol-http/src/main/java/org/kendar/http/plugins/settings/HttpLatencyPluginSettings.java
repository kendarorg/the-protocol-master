package org.kendar.http.plugins.settings;

import org.kendar.plugins.settings.LatencyPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpLatencyPluginSettings extends LatencyPluginSettings {

    private List<String> target = new ArrayList<>();

    public List<String> getTarget() {
        return target;
    }

    public void setTarget(List<String> target) {
        this.target = target;
    }
}
