package org.kendar.storage;

/**
 * Base interface for storage
 *
 * @param <I>
 * @param <O>
 */
public interface Storage<I, O> {
    /**
     * Initialize the storage
     */
    void initialize();

    Storage<I, O> withFullData();

    /**
     * Write a storage item
     *
     * @param connectionId from context
     * @param request
     * @param response
     * @param durationMs
     * @param type
     * @param caller
     */
    void write(int connectionId, I request, O response, long durationMs, String type, String caller);

    /**
     * Before closing the server optimize the written data
     */
    void optimize();
}
