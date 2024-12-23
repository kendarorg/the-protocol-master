package org.kendar.amqp.v09.apis.dtos;

public class AmqpConnection {
    private Integer id;
    private Short channel;
    private String consumeOrigin;
    private Object consumerTag;
    private boolean canPublish;
    private int consumeId;
    private String exchange;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setChannel(Short channel) {
        this.channel = channel;
    }

    public Short getChannel() {
        return channel;
    }

    public void setConsumeOrigin(String consumeOrigin) {
        this.consumeOrigin = consumeOrigin;
    }

    public String getConsumeOrigin() {
        return consumeOrigin;
    }

    public void setConsumerTag(Object consumerTag) {
        this.consumerTag = consumerTag;
    }

    public Object getConsumerTag() {
        return consumerTag;
    }

    public void setCanPublish(boolean canPublish) {
        this.canPublish = canPublish;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getExchange() {
        return exchange;
    }
}
