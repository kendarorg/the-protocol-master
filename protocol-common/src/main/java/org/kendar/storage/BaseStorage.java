package org.kendar.storage;

import org.kendar.protocol.descriptor.ProtoDescriptor;

/**
 * Base class for the storage
 *
 * @param <I>
 * @param <O>
 */
public abstract class BaseStorage<I, O> implements Storage<I, O> {
    protected boolean useFullData = false;

    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    public abstract void initialize();

    public void write(int connectionId, I request, O response, long durationMs, String type, String caller) {
        var item = new StorageItem(connectionId, request, response, durationMs, type, caller);
        write(item);
    }

    public void write(long index,int connectionId, I request, O response, long durationMs, String type, String caller) {
        var item = new StorageItem(index,connectionId, request, response, durationMs, type, caller);
        write(item);
    }

    protected abstract void write(StorageItem item);

    public Storage<I, O> withFullData() {
        this.useFullData = true;
        return this;
    }

    public long generateIndex(){
        return ProtoDescriptor.getCounter("STORAGE_ID");
    }


}
