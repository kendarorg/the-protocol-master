package org.kendar.storage;

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
    protected void write(StorageItem item) {

    }
}
