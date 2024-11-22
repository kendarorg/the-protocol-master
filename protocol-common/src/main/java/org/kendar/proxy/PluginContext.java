package org.kendar.proxy;

import org.kendar.protocol.context.ProtoContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class PluginContext {
    private static final AtomicLong counter = new AtomicLong(0);
    private final String type;
    private final ProtoContext context;
    private long index;
    private long start;
    private String caller;

    public Map<String, Object> getTags() {
        return tags;
    }

    private final Map<String,Object> tags = new HashMap<>();

    public PluginContext(String caller, String type, long start, ProtoContext context) {

        this.caller = caller;
        this.type = type;
        this.context = context;
        this.index = counter.incrementAndGet();
        this.start = start;
    }

    public ProtoContext getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public int getContextId() {
        if (getContext() == null) return 0;
        return getContext().getContextId();
    }
}
