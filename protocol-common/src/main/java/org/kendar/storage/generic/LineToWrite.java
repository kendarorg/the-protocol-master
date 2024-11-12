package org.kendar.storage.generic;

import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

public class LineToWrite {
    private String instanceId;
    private CompactLine compactLine;
    private StorageItem storageItem;

    public LineToWrite(String instanceId, StorageItem storageItem, CompactLine compactLine) {
        this.instanceId = instanceId;
        this.compactLine = compactLine;
        this.storageItem = storageItem;
    }

    public LineToWrite(String instanceId, CompactLine compactLine) {

        this.instanceId = instanceId;
        this.compactLine = compactLine;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public CompactLine getCompactLine() {
        return compactLine;
    }

    public void setCompactLine(CompactLine compactLine) {
        this.compactLine = compactLine;
    }

    public StorageItem getStorageItem() {
        return storageItem;
    }

    public void setStorageItem(StorageItem storageItem) {
        this.storageItem = storageItem;
    }
}
