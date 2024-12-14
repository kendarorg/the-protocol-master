package org.kendar.resp3.pubsub;

import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class Publisher {

    private static final Logger logger = LoggerFactory.getLogger(Publisher.class);

    private final Jedis publisherJedis;

    private final String channel;

    public Publisher(Jedis publisherJedis, String channel) {
        this.publisherJedis = publisherJedis;
        this.channel = channel;
    }

    public void start(String... data) {
        //logger.info("Type your message (quit for terminate)");

        //BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (var line : data) {
            System.out.println("Sending " + line);
            publisherJedis.publish(channel, line);
            System.out.println("Published " + line);
            Sleeper.sleep(100);
        }

    }
}
