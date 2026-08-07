package org.kendar.amqp.v10.plugins.apis.dtos;

/**
 * Payload for the publish endpoint. A binary content type carries the body as a
 * base-64 string and is delivered as a {@code data} section; otherwise the body
 * is delivered as an {@code amqp-value} string (the qpid-jms TextMessage mapping).
 */
public class PublishAmqp10Message {
    private String contentType = "text/plain";
    private String appId;
    private String body;
    private long deliveryTag = 1;
    private String source = null;

    public PublishAmqp10Message() {
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

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
