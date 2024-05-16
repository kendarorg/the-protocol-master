package org.kendar.resp3;

import org.junit.jupiter.api.*;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleTest extends BasicTest{
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
    void resp2() throws Exception {
        JedisPool pool = new JedisPool("127.0.0.1", FAKE_PORT);

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
    }



    @Test
    void resp3() throws Exception {
        HostAndPort hnp = HostAndPort.from("127.0.0.1:"+FAKE_PORT);

        try (Jedis jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().resp3().build())) {
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
    }
}
