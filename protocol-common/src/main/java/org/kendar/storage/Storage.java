package org.kendar.storage;

public interface Storage<I, O> {
    void initialize();

    void write(I request, O response, long durationMs, String type, String caller);
}
