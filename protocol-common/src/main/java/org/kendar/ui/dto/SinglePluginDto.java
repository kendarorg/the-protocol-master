package org.kendar.ui.dto;

import org.kendar.settings.PluginSettings;

public class SinglePluginDto extends PluginDto {
    private String settings;
    private PluginSettings settingsObject;

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public PluginSettings getSettingsObject() {
        return settingsObject;
    }

    public void setSettingsObject(PluginSettings settingsObject) {
        this.settingsObject = settingsObject;
    }
}
