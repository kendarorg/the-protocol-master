package org.kendar.mqtt.apis.dtos;

public class PublishMessage {
    private String contentType;
    private byte fullFlag = 48;
    private String body;
    private String topic;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public byte getFullFlag() {
        return fullFlag;
    }

    public void setFullFlag(byte fullFlag) {
        this.fullFlag = fullFlag;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
