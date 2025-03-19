package org.kendar.plugins.apis.dtos;

import org.kendar.utils.ReplacerItem;

public class ReplacerItemFile {
    private String fileName;
    private ReplacerItem replacerItem;
    private String instanceId;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public ReplacerItemFile(String instanceId,String fileName, ReplacerItem replacerItem) {
        this.instanceId = instanceId;
        this.fileName = fileName;
        this.replacerItem = replacerItem;
    }

    public ReplacerItemFile() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ReplacerItem getReplacerItem() {
        return replacerItem;
    }

    public void setReplacerItem(ReplacerItem replacerItem) {
        this.replacerItem = replacerItem;
    }
}
