package org.kendar.storage;

import org.kendar.protocol.descriptor.ProtoDescriptor;

import java.util.List;

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
     * Write a storage item
     *
     * @param index        index of the item to write
     * @param connectionId from context
     * @param request
     * @param response
     * @param durationMs
     * @param type
     * @param caller
     */
    void write(long index, int connectionId, I request, O response, long durationMs, String type, String caller);

    /**
     * Before closing the server optimize the written data
     */
    void optimize();

    /**
     * Reserve an index
     *
     * @return
     */
    long generateIndex();

    StorageItem<I, O> read(I toRead, String type);

    List<StorageItem<I, O>> readResponses(long afterIndex);

    ProtoDescriptor getDescriptor();

    void setDescriptor(ProtoDescriptor protocol);
}
