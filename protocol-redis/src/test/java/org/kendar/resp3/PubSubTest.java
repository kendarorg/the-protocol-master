package org.kendar.resp3;

import org.junit.jupiter.api.*;
import org.kendar.resp3.pubsub.Publisher;
import org.kendar.resp3.pubsub.Subscriber;
import org.kendar.utils.Sleeper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PubSubTest extends BasicTest {
    public static final String CHANNEL_NAME = "commonChannel";

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void pubsub() throws Exception {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        final JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", FAKE_PORT, 0);
        final Jedis subscriberJedis = jedisPool.getResource();
        final Subscriber subscriber = new Subscriber();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    subscriberJedis.subscribe(subscriber, CHANNEL_NAME);
                } catch (Exception e) {
                    System.err.println("Subscribing failed.");
                }
            }
        }).start();

        final Jedis publisherJedis = jedisPool.getResource();

        new Publisher(publisherJedis, CHANNEL_NAME).start("FIRST", "SECOND", "THIRD");
        //new Publisher(publisherJedis, CHANNEL_NAME).start("FIRST");

        Sleeper.sleep(500);
        subscriber.unsubscribe();
        jedisPool.returnResource(subscriberJedis);
        jedisPool.returnResource(publisherJedis);
        Sleeper.sleep(500);
        assertEquals(3, subscriber.results.size());
        assertTrue(subscriber.results.containsKey("FIRST"));
        assertTrue(subscriber.results.containsKey("SECOND"));
        assertTrue(subscriber.results.containsKey("THIRD"));
    }
}
