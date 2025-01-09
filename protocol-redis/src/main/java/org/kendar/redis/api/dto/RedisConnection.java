package org.kendar.redis.api.dto;

public class RedisConnection {
    private Integer id;
    private String subscription;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public String getSubscription() {
        return subscription;
    }
}
