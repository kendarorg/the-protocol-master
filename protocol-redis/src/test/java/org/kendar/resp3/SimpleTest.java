package org.kendar.resp3;

import org.junit.jupiter.api.Test;
import org.kendar.testcontainer.images.RedisImage;
import org.kendar.utils.Sleeper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleTest {
    @Test
    void test() throws Exception {
        var image = new RedisImage();
        image.start();
        Sleeper.sleep(1000);
        JedisPool pool = new JedisPool(image.getHost(), image.getPort());

        try (Jedis jedis = pool.getResource()) {
            // Store & Retrieve a simple string
            jedis.set("foo", "bar");
            assertEquals ("bar",jedis.get("foo").toString()); // prints bar

            // Store & Retrieve a HashMap
            Map<String, String> hash = new HashMap<>();;
            hash.put("name", "John");
            hash.put("surname", "Smith");
            hash.put("company", "Redis");
            hash.put("age", "29");
            jedis.hset("user-session:123", hash);
            assertEquals("{name=John, surname=Smith, company=Redis, age=29}",jedis.hgetAll("user-session:123").toString());
            // Prints: {name=John, surname=Smith, company=Redis, age=29}
        }
        image.close();
    }
}
