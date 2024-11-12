package org.kendar.storage.generic;

import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

public class LineToRead {
    private final StorageItem storageItem;
    private final CompactLine compactLine;

    public StorageItem getStorageItem() {
        return storageItem;
    }

    public CompactLine getCompactLine() {
        return compactLine;
    }

    public LineToRead(StorageItem storageItem, CompactLine compactLine) {

        this.storageItem = storageItem;
        this.compactLine = compactLine;
    }
}
