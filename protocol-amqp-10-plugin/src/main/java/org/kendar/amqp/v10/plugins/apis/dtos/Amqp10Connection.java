package org.kendar.amqp.v10.plugins.apis.dtos;

import org.kendar.utils.JsonMapper;

import java.util.Objects;

/**
 * A consumer delivery link exposed by the publish plugin: one AMQP 1.0 receiver
 * link the proxy can inject transfers onto (analog of v09's {@code AmqpConnection}).
 */
public class Amqp10Connection {
    private Integer id;
    private Short channel;
    private Long handle;
    private String linkName;
    private String source;
    private boolean canPublish;
    private long lastAccess;

    public String serialized() {
        return new JsonMapper().serialize(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Amqp10Connection that = (Amqp10Connection) o;
        return canPublish == that.canPublish && Objects.equals(id, that.id)
                && Objects.equals(channel, that.channel) && Objects.equals(handle, that.handle)
                && Objects.equals(linkName, that.linkName) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channel, handle, linkName, source, canPublish);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Short getChannel() {
        return channel;
    }

    public void setChannel(Short channel) {
        this.channel = channel;
    }

    public Long getHandle() {
        return handle;
    }

    public void setHandle(Long handle) {
        this.handle = handle;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public void setCanPublish(boolean canPublish) {
        this.canPublish = canPublish;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
}
