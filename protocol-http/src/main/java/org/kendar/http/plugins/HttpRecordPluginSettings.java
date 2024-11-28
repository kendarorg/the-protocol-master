package org.kendar.http.plugins;

import org.kendar.plugins.settings.BasicRecordPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpRecordPluginSettings extends BasicRecordPluginSettings {
    private boolean removeEtags = true;
    private List<String> recordSites = new ArrayList<>();

    public boolean isRemoveEtags() {
        return removeEtags;
    }

    public void setRemoveEtags(boolean removeEtags) {
        this.removeEtags = removeEtags;
    }

    public List<String> getRecordSites() {
        return recordSites;
    }

    public void setRecordSites(List<String> recordSites) {
        this.recordSites = recordSites;
    }
}
