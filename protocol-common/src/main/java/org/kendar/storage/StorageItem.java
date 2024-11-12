package org.kendar.storage;

import org.kendar.utils.JsonMapper;

/**
 * Storage item
 *
 * @param <I>
 * @param <O>
 */
public class StorageItem {
    private static final JsonMapper mapper = new JsonMapper();
    private boolean constant;
    private int connectionId;
    private long index = -1;
    private Object input;
    private Object output;
    private long durationMs;
    private String type;
    private String caller;
    private int nth = -1;
    private Object deserializedInput;
    private Object deserializedOutput;

    /**
     * Needed for serialization
     */
    public StorageItem() {

    }

    public StorageItem(int connectionId, Object input, Object output, long durationMs, String type, String caller) {
        this.connectionId = connectionId;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.type = type;
        this.caller = caller;
    }

    public StorageItem(long index, int connectionId, Object input, Object output, long durationMs, String type, String caller) {
        this.index = index;
        this.connectionId = connectionId;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.type = type;
        this.caller = caller;
    }

    public <T,K> void initializeIo(Class<T> typeIn,Class<K> typeOut) {
        if(typeIn!=null && input!=null && input.getClass()!=typeIn)input = mapper.deserialize(mapper.serialize(this.getInput()), typeIn);
        if(typeOut!=null && output!=null  && output.getClass()!=typeOut)output = mapper.deserialize(mapper.serialize(this.getOutput()), typeOut);
    }

    public int getNth() {
        return nth;
    }

    public void setNth(int nth) {
        this.nth = nth;
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

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
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
