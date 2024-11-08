package org.kendar.proxy;

import org.kendar.protocol.context.ProtoContext;

import java.util.concurrent.atomic.AtomicLong;

public class FilterContext {
    private long index;
    private long start;
    private String caller;
    private final String type;
    private final ProtoContext context;
    private static AtomicLong counter = new AtomicLong(0);

    public ProtoContext getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public FilterContext(String caller, String type, long start, ProtoContext context) {

        this.caller = caller;
        this.type = type;
        this.context = context;
        this.index = counter.incrementAndGet();
        this.start = start;
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
}
