package org.kendar.resp3.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ConcurrentHashMap;

public class Subscriber extends JedisPubSub {

    private static Logger log = LoggerFactory.getLogger(Subscriber.class);
    public ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();

    @Override
    public void onMessage(String channel, String message) {
        log.debug("===========Message received. Channel: {}, Msg: {}", channel, message);
        results.put(message, channel);
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }
}