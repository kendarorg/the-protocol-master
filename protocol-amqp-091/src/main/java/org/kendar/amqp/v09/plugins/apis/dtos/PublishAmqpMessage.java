package org.kendar.amqp.v09.plugins.apis.dtos;

public class PublishAmqpMessage {
    private String contentType;
    private String appId;
    private String body;
    private int propertyFlag = -28664;
    private int deliveryMode = 1;
    private long deliveryTag = 1;
    private String queue = null;
    private String exchange = null;

    public PublishAmqpMessage() {

    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public int getPropertyFlag() {
        return propertyFlag;
    }

    public void setPropertyFlag(int propertyFlag) {
        this.propertyFlag = propertyFlag;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public void setDeliveryMode(Integer deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }
}
