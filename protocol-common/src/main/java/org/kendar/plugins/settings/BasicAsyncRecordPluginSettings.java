package org.kendar.plugins.settings;

public class BasicAsyncRecordPluginSettings extends BasicRecordPluginSettings {
    private boolean resetConnectionsOnStart = true;

    public boolean isResetConnectionsOnStart() {
        return resetConnectionsOnStart;
    }

    public void setResetConnectionsOnStart(boolean resetConnectionsOnStart) {
        this.resetConnectionsOnStart = resetConnectionsOnStart;
    }
}
