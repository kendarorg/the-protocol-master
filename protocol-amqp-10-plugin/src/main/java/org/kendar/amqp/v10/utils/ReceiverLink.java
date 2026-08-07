package org.kendar.amqp.v10.utils;

/**
 * Consume-correlation DTO (the AMQP 1.0 analog of v09's {@code BasicConsume}
 * bookkeeping). Populated from the broker's {@code attach} response to a client
 * consumer: the broker attaches as {@code sender} on {@code handle}, over session
 * {@code channel}, delivering the queue named by {@code source}. The publish
 * plugin injects {@code transfer} frames onto exactly this (channel, handle) pair
 * so they reach the connected consumer.
 */
public class ReceiverLink {
    private String name;
    private long handle;
    private short channel;
    private String source;
    private long lastAccess;

    public ReceiverLink() {
    }

    public ReceiverLink(String name, long handle, short channel, String source) {
        this.name = name;
        this.handle = handle;
        this.channel = channel;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getHandle() {
        return handle;
    }

    public void setHandle(long handle) {
        this.handle = handle;
    }

    public short getChannel() {
        return channel;
    }

    public void setChannel(short channel) {
        this.channel = channel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
}
