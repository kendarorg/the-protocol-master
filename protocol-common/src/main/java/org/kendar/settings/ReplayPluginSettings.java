package org.kendar.settings;

public class ReplayPluginSettings extends PluginSettings {
    private boolean replay;
    private String replayId;
    private boolean respectTimings;

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public boolean isRespectTimings() {
        return respectTimings;
    }

    public void setRespectTimings(boolean respectTimings) {
        this.respectTimings = respectTimings;
    }

    public boolean isReplay() {
        return replay;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }
}
