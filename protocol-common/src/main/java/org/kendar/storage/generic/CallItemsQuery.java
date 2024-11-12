package org.kendar.storage.generic;

import java.util.HashMap;
import java.util.HashSet;

public class CallItemsQuery {
    private String type;
    private HashSet<Integer> used = new HashSet<>();
    private HashMap<String, String> tags = new HashMap<>();
    private String caller;

    public HashMap<String, String> getTags() {
        return tags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if(type.equalsIgnoreCase("bbuffer")){
            type="byte[]";
        }
        this.type = type;
    }

    public HashSet<Integer> getUsed() {
        return used;
    }

    public void setUsed(HashSet<Integer> used) {
        this.used = used;
    }

    public void addTag(String tag, Object value) {
        tags.put(tag, value != null ? value.toString() : null);
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getTag(String tag) {
        return tags.get(tag);
    }
}
