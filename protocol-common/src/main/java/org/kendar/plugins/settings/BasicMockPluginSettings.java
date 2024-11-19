package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicMockPluginSettings extends PluginSettings {
    private String dataDir;

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
