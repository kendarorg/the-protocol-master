package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicPercentPluginSettings extends PluginSettings {
    public int getPercentAction() {
        return percentAction;
    }

    public void setPercentAction(int percentAction) {
        this.percentAction = percentAction;
    }

    private int percentAction=50;
}
