package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicReplayPluginSettings extends PluginSettings {
    private boolean respectCallDuration;
    private String replayId;

    private boolean blockExternal=true;

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public boolean isRespectCallDuration() {
        return respectCallDuration;
    }

    public void setRespectCallDuration(boolean respectCallDuration) {
        this.respectCallDuration = respectCallDuration;
    }

    public boolean isBlockExternal() {
        return blockExternal;
    }

    public void setBlockExternal(boolean blockExternal) {
        this.blockExternal = blockExternal;
    }
}
