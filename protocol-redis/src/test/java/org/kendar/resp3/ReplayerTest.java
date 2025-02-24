package org.kendar.resp3;

import org.junit.jupiter.api.Test;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.plugins.RedisReplayPlugin;
import org.kendar.resp3.pubsub.Publisher;
import org.kendar.resp3.pubsub.Subscriber;
import org.kendar.settings.ByteProtocolSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplayerTest {

    public static final String CHANNEL_NAME = "commonChannel";
    protected static final int FAKE_PORT = 6389;

    @Test
    void testReplayer() {
        var baseProtocol = new Resp3Protocol(FAKE_PORT);
        var proxy = new Resp3Proxy();
        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "replay"));
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);

        var pl = new RedisReplayPlugin(new JsonMapper(), storage).initialize(gs, new ByteProtocolSettings(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.of(pl));
        pl.setActive(true);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        try {
            protocolServer.start();
            Sleeper.sleep(5000, protocolServer::isRunning);


            final JedisPoolConfig poolConfig = new JedisPoolConfig();
            final JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", FAKE_PORT, 0);
            final Jedis subscriberJedis = jedisPool.getResource();
            final Subscriber subscriber = new Subscriber();

            new Thread(() -> {
                try {
                    subscriberJedis.subscribe(subscriber, CHANNEL_NAME);
                } catch (Exception e) {
                    System.err.println("Subscribing failed.");
                }
            }).start();

            Sleeper.sleep(1000);

            final Jedis publisherJedis = jedisPool.getResource();

            new Thread(() -> new Publisher(publisherJedis, CHANNEL_NAME).start("FIRST", "SECOND", "THIRD")).start();


            Sleeper.sleepNoException(3000, () -> subscriber.results.size() == 3);
            subscriber.unsubscribe();
            jedisPool.returnResource(subscriberJedis);
            jedisPool.returnResource(publisherJedis);
            Sleeper.sleepNoException(3000, () -> subscriber.results.size() == 3);
            assertEquals(3, subscriber.results.size());
            assertTrue(subscriber.results.containsKey("FIRST"));
            assertTrue(subscriber.results.containsKey("SECOND"));
            assertTrue(subscriber.results.containsKey("THIRD"));
        } finally {
            protocolServer.stop();
        }
    }
}
