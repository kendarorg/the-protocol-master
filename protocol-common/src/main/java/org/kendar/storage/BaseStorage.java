package org.kendar.storage;

/**
 * Base class for the storage
 *
 * @param <I>
 * @param <O>
 */
public abstract class BaseStorage<I, O> implements Storage<I, O> {
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

    public void write(I request, O response, long durationMs, String type, String caller) {
        var item = new StorageItem(request, response, durationMs, type, caller);
        write(item);
    }

    protected abstract void write(StorageItem item);


}
