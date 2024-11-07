package org.kendar.http.plugins;

import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpRecordPluginSettings extends PluginSettings {
    private boolean record;
    private List<String> recordSites = new ArrayList<>();

    public List<String> getRecordSites() {
        return recordSites;
    }

    public void setRecordSites(List<String> recordSites) {
        this.recordSites = recordSites;
    }

    public boolean isRecord() {
        return record;
    }

    public void setRecord(boolean record) {
        this.record = record;
    }
}
