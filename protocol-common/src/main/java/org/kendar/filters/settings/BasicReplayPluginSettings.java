package org.kendar.filters.settings;

import org.kendar.settings.PluginSettings;

public class BasicReplayPluginSettings extends PluginSettings {
    private boolean respectCallDuration;
    public String getReplayId() {
        return replayId;
    }
    private String replayId;

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public boolean isRespectCallDuration() {
        return respectCallDuration;
    }

    public void setRespectCallDuration(boolean respectCallDuration) {
        this.respectCallDuration = respectCallDuration;
    }

}
