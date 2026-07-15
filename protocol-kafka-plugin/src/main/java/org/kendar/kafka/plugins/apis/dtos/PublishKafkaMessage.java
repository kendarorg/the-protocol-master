package org.kendar.kafka.plugins.apis.dtos;

/**
 * Payload for the Kafka publish endpoint. A binary content type carries the body
 * (and key) as base-64 strings; otherwise they are UTF-8. The message is produced
 * for real through the proxy's upstream connection (protocol-kafka.md §7).
 */
public class PublishKafkaMessage {
    private String contentType = "text/plain";
    private String topic;
    private int partition = 0;
    private String key;
    private String body;
    private short acks = 1;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public short getAcks() {
        return acks;
    }

    public void setAcks(short acks) {
        this.acks = acks;
    }
}
