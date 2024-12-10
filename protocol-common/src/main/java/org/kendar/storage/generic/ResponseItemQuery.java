package org.kendar.storage.generic;

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


}
