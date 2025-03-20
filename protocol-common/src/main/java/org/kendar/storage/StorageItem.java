package org.kendar.storage;

import org.kendar.utils.JsonMapper;

/**
 * Storage item
 *
 * @param <I>
 * @param <O>
 */
public class StorageItem {
    protected static final JsonMapper mapper = new JsonMapper();
    private boolean trivial = false;
    private long timestamp = System.currentTimeMillis();
    private boolean constant;
    private int connectionId;
    private long index = -1;
    private Object input;
    private Object output;
    private long durationMs;
    private String type;
    private String caller;
    private Object deserializedInput;
    private Object deserializedOutput;
    private Object outAs;
    private Object inAs;
    private String inputType;
    private String outputType;

    public boolean isTrivial() {
        return trivial;
    }

    public void setTrivial(boolean trivial) {
        this.trivial = trivial;
    }

    /**
     * Needed for serialization
     */
    public StorageItem() {

    }

    public StorageItem(int connectionId, Object input, Object output, long durationMs, String type, String caller,
                       String inputType, String outputType) {
        this.connectionId = connectionId;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.type = type;
        this.caller = caller;
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public StorageItem(long index, int connectionId, Object input, Object output, long durationMs, String type, String caller,
                       String inputType, String outputType) {
        this.index = index;
        this.connectionId = connectionId;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.type = type;
        this.caller = caller;
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public <T> T retrieveInAs(Class<T> clazz) {
        if (inAs == null && input != null) {
            if (clazz == input.getClass()) {
                inAs = input;
            } else {
                inAs = mapper.deserialize(input, clazz);
            }
        }
        return (T) inAs;
    }

    public <T> T retrieveOutAs(Class<T> clazz) {
        if (outAs == null && output != null) {
            if (clazz == output.getClass()) {
                outAs = output;
            } else {
                outAs = mapper.deserialize(output, clazz);
            }
        }
        return (T) outAs;
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
        this.input = mapper.toJsonNode(input);
        inAs = null;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = mapper.toJsonNode(output);
        outAs = null;
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
