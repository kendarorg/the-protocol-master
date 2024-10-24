package org.kendar.storage;

import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;

import java.util.List;

public class NullStorageRepository<I, O> implements StorageRepository<I, O> {
    @Override
    public void initialize(BaseStorage<I, O> baseStorage) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void write(StorageItem item) {

    }

    @Override
    public void optimize() {

    }

    @Override
    public StorageItem read(CallItemsQuery query) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<StorageItem> readResponses(ResponseItemQuery query) {
        throw new RuntimeException("Not implemented");
    }
}
