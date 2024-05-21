package org.kendar.storage;

import java.util.List;

/**
 * Do-nothing storage
 *
 * @param <I>
 * @param <O>
 */
public class NullStorage<I, O> extends BaseStorage<I, O> {
    @Override
    public void initialize() {

    }

    @Override
    public Storage<I, O> withFullData() {
        return this;
    }

    @Override
    public void optimize() {

    }

    @Override
    public StorageItem<I, O> read(I toRead, String type) {
        return null;
    }

    @Override
    public List<StorageItem<I, O>> readResponses(long afterIndex) {
        return List.of();
    }

    @Override
    protected void write(StorageItem item) {

    }
}
