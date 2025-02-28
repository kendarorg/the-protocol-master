package org.kendar.http.plugins.settings;

import org.kendar.plugins.settings.BasicReplayPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpReplayPluginSettings extends BasicReplayPluginSettings {

    private List<String> target = new ArrayList<>();

    public List<String> getTarget() {
        return target;
    }

    public void setTarget(List<String> target) {
        this.target = target;
    }
}
