package org.kendar.storage;

public class StorageFileIndex {
    private String index;
    private String instanceId;
    private String pluginId;

    public StorageFileIndex() {
    }

    public StorageFileIndex(String instanceId, String pluginId, String index) {
        this.instanceId = instanceId;
        this.pluginId = pluginId;
        this.index = index;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
}
