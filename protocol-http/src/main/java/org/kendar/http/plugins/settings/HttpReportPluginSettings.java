package org.kendar.http.plugins.settings;

import org.kendar.settings.PluginSettings;

public class HttpReportPluginSettings extends PluginSettings {
    private boolean ignoreTpm = true;


    public boolean isIgnoreTpm() {
        return ignoreTpm;
    }

    public void setIgnoreTpm(boolean ignoreTpm) {
        this.ignoreTpm = ignoreTpm;
    }
}
