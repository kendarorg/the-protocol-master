package org.kendar.plugins.settings;

import org.kendar.settings.PluginSettings;

public class BasicReplayPluginSettings extends PluginSettings {
    private boolean respectCallDuration;
    private String replayId;

    private boolean blockExternal = true;
    private boolean ignoreTrivialCalls = true;

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public boolean isIgnoreTrivialCalls() {
        return ignoreTrivialCalls;
    }

    public void setIgnoreTrivialCalls(boolean ignoreTrivialCalls) {
        this.ignoreTrivialCalls = ignoreTrivialCalls;
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
