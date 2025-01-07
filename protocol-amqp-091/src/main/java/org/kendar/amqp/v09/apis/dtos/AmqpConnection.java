package org.kendar.amqp.v09.apis.dtos;

public class AmqpConnection {
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
