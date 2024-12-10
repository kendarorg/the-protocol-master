package org.kendar.storage.generic;

import java.util.HashMap;
import java.util.HashSet;

public class ResponseItemQuery {
    private HashSet<Integer> used = new HashSet<>();
    private String caller;
    private long startAt;

    public HashSet<Integer> getUsed() {
        return used;
    }

    public void setUsed(HashSet<Integer> used) {
        this.used = used;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public long getStartAt() {
        return startAt;
    }

    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }

    private final HashMap<String, String> tags = new HashMap<>();
    public HashMap<String, String> getTags() {
        return tags;
    }
    public String getTag(String tag) {
        return tags.get(tag);
    }

    public void addTag(String tag, Object value) {
        tags.put(tag, value != null ? value.toString() : null);
    }


}
