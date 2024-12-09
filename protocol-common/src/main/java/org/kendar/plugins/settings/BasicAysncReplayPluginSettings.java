package org.kendar.plugins.settings;

public class BasicAysncReplayPluginSettings extends BasicReplayPluginSettings {
    private boolean resetConnectionsOnStart = false;

    public boolean isResetConnectionsOnStart() {
        return resetConnectionsOnStart;
    }

    public void setResetConnectionsOnStart(boolean resetConnectionsOnStart) {
        this.resetConnectionsOnStart = resetConnectionsOnStart;
    }
}
