package org.kendar.settings;

public class DefaultSimulationSettings {
    private boolean replay;
    private boolean respectCallDuration;
    private String replayId;
    private boolean record;
    private boolean mock;

    public boolean isReplay() {
        return replay;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }

    public boolean isRespectCallDuration() {
        return respectCallDuration;
    }

    public void setRespectCallDuration(boolean respectCallDuration) {
        this.respectCallDuration = respectCallDuration;
    }

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public boolean isRecord() {
        return record;
    }

    public void setRecord(boolean record) {
        this.record = record;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }
}
