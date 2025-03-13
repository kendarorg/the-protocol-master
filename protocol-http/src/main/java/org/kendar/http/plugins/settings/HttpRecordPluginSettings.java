package org.kendar.http.plugins.settings;

import org.kendar.plugins.settings.BasicRecordPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpRecordPluginSettings extends BasicRecordPluginSettings {
    private boolean removeEtags = true;
    private List<String> target = new ArrayList<>();

    public boolean isRemoveEtags() {
        return removeEtags;
    }

    public void setRemoveEtags(boolean removeEtags) {
        this.removeEtags = removeEtags;
    }

    public List<String> getTarget() {
        return target;
    }

    public void setTarget(List<String> target) {
        this.target = target;
    }
}
