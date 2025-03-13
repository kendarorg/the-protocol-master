package org.kendar.http.plugins.settings;

import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpReportPluginSettings extends PluginSettings {
    private boolean ignoreTpm = true;
    private List<String> ignore = new ArrayList<>();

    public List<String> getIgnore() {
        return ignore;
    }

    public void setIgnore(List<String> ignore) {
        this.ignore = ignore;
    }

    public boolean isIgnoreTpm() {
        return ignoreTpm;
    }

    public void setIgnoreTpm(boolean ignoreTpm) {
        this.ignoreTpm = ignoreTpm;
    }
}
