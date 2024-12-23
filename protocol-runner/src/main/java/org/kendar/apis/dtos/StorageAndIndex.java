package org.kendar.apis.dtos;

import org.kendar.storage.StorageItem;

public class StorageAndIndex {
    private StorageItem item;
    private CompactLineApi index;

    public StorageItem getItem() {
        return item;
    }

    public void setItem(StorageItem item) {
        this.item = item;
    }

    public CompactLineApi getIndex() {
        return index;
    }

    public void setIndex(CompactLineApi index) {
        this.index = index;
    }
}
