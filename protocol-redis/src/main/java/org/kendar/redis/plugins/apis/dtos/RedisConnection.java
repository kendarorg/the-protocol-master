package org.kendar.redis.plugins.apis.dtos;

import java.util.Objects;

public class RedisConnection {
    private Integer id;
    private String subscription;

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", subscription='" + subscription + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RedisConnection that = (RedisConnection) o;
        return Objects.equals(id, that.id) && Objects.equals(subscription, that.subscription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subscription);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }
}
