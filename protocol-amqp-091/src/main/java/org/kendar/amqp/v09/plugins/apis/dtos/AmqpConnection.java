package org.kendar.amqp.v09.plugins.apis.dtos;

import org.kendar.utils.JsonMapper;

import java.util.Objects;

public class AmqpConnection {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AmqpConnection that = (AmqpConnection) o;
        return canPublish == that.canPublish && consumeId == that.consumeId && Objects.equals(id, that.id) && Objects.equals(channel, that.channel) && Objects.equals(consumeOrigin, that.consumeOrigin) && Objects.equals(consumerTag, that.consumerTag) && Objects.equals(exchange, that.exchange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channel, consumeOrigin, consumerTag, canPublish, consumeId, exchange);
    }

    public String serialized(){
        return new JsonMapper().serialize(this);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", channel=" + channel +
                ", consumeOrigin='" + consumeOrigin + '\'' +
                ", consumerTag=" + consumerTag +
                ", canPublish=" + canPublish +
                ", consumeId=" + consumeId +
                ", exchange='" + exchange + '\'' +
                '}';
    }

    private Integer id;
    private Short channel;
    private String consumeOrigin;
    private Object consumerTag;
    private boolean canPublish;
    private int consumeId;
    private String exchange;

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

    public String getConsumeOrigin() {
        return consumeOrigin;
    }

    public void setConsumeOrigin(String consumeOrigin) {
        this.consumeOrigin = consumeOrigin;
    }

    public Object getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(Object consumerTag) {
        this.consumerTag = consumerTag;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public void setCanPublish(boolean canPublish) {
        this.canPublish = canPublish;
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
}
