package org.kendar.events;

public class ReplayStatusEvent  implements TpmEvent{
    private final boolean replaying;

    public boolean isReplaying() {
        return replaying;
    }

    public ReplayStatusEvent(boolean replaying){

        this.replaying = replaying;
    }
}
