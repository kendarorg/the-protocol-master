package org.kendar.storage;

public interface StorageRoot<I, O> {
    void initialize();

    void write(I request, O response, long durationMs, String type, String caller);
}
