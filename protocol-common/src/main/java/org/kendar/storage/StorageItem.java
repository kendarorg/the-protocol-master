package org.kendar.storage;

/**
 * Storage item
 *
 * @param <I>
 * @param <O>
 */
public class StorageItem<I, O> {
    private boolean constant;
    private int connectionId;
    private long index = -1;
    private I input;
    private O output;
    private long durationMs;
    private String type;
    private String caller;

    /**
     * Needed for serialization
     */
    public StorageItem() {

    }

    public StorageItem(int connectionId, I input, O output, long durationMs, String type, String caller) {
        this.connectionId = connectionId;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.type = type;
        this.caller = caller;
    }

    public StorageItem(long index, int connectionId, I input, O output, long durationMs, String type, String caller) {
        this.index = index;
        this.connectionId = connectionId;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.type = type;
        this.caller = caller;
    }

    public boolean isConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public I getInput() {
        return input;
    }

    public void setInput(I input) {
        this.input = input;
    }

    public O getOutput() {
        return output;
    }

    public void setOutput(O output) {
        this.output = output;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }
}
