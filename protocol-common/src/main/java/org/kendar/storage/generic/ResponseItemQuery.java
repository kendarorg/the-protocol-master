package org.kendar.storage.generic;

import java.util.HashSet;

public class ResponseItemQuery {
    private HashSet<Integer> used = new HashSet<>();
    private String caller;
    private long startAt;


    public void setUsed(HashSet<Integer> used) {
        this.used = used;
    }

    public HashSet<Integer> getUsed() {
        return used;
    }


    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getCaller() {
        return caller;
    }

    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }

    public long getStartAt() {
        return startAt;
    }
}
