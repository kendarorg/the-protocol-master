package org.kendar.amqp.v09.apis.dtos;

public class PublishMessage {
    private String contentType;
    private String appId;
    private String body;


    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    private int propertyFlag = -28664;
    private int deliveryMode = 1;
    private long deliveryTag = 1;

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
