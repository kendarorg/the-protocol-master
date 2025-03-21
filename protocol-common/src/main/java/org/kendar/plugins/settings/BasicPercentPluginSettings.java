package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicPercentPluginSettings extends PluginSettings {
    private int percentAction = 50;

    public int getPercentAction() {
        return percentAction;
    }

    public void setPercentAction(int percentAction) {
        this.percentAction = percentAction;
    }

    public BasicPercentPluginSettings withPercentAction(int percentAction) {
        setPercentAction(percentAction);
        return this;
    }
}
