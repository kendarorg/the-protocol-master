package org.kendar.storage.generic;

import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

public class LineToWrite {
    private final String instanceId;
    private final CompactLine compactLine;
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

    public CompactLine getCompactLine() {
        return compactLine;
    }

    public StorageItem getStorageItem() {
        return storageItem;
    }
}
