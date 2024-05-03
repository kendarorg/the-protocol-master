package org.kendar.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CompactLine {

    private long index;
    private String type;
    private String caller;
    private long durationMs;
    private Map<String, String> tags = new HashMap<>();

    public CompactLine() {

    }


    public CompactLine(StorageItem item, Supplier<Map<String, String>> getTags) {
        index = item.getIndex();
        type = item.getType();
        caller = item.getCaller();
        durationMs = item.getDurationMs();
        tags = getTags.get();
    }


    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
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

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
