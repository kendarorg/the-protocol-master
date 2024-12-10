package org.kendar.storage.generic;

import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

public class LineToWrite {
    private final String instanceId;
    private final CompactLine compactLine;
    private final long id;
    private StorageItem storageItem;

    public LineToWrite(String instanceId, StorageItem storageItem, CompactLine compactLine, long id) {
        this.instanceId = instanceId;
        this.compactLine = compactLine;
        this.storageItem = storageItem;
        this.id = id;
    }

    public LineToWrite(String instanceId, CompactLine compactLine, long id) {

        this.instanceId = instanceId;
        this.compactLine = compactLine;
        this.id = id;
    }

    public long getId() {
        return id;
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

    public long retrieveTimestamp(){
        return storageItem.getTimestamp();
    }
}
