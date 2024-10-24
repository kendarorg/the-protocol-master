package org.kendar.storage.generic;

import org.kendar.storage.BaseStorage;
import org.kendar.storage.StorageItem;

import java.util.List;

public interface StorageRepository<I, O> {
    void initialize(BaseStorage<I,O> baseStorage);
    void flush();
    void write(StorageItem item);
    void optimize();
    StorageItem read(CallItemsQuery query);
    List<StorageItem> readResponses(ResponseItemQuery query);
}
