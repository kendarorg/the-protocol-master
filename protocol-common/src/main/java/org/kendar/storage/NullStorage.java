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
    protected void write(StorageItem item) {

    }
}
