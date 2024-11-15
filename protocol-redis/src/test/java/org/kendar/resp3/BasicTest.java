package org.kendar.resp3;


import org.junit.jupiter.api.TestInfo;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.plugins.RedisRecordingPlugin;
import org.kendar.server.TcpServer;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.testcontainer.images.RedisImage;
import org.kendar.testcontainer.utils.Utils;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 6389;
    protected static RedisImage redisImage;
    protected static TcpServer protocolServer;

    public static void beforeClassBase() {
        //LoggerBuilder.setLevel(Logger.ROOT_LOGGER_NAME, Level.DEBUG);

        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        redisImage = new RedisImage();
        redisImage
                .withNetwork(network)
                .waitingForPort(6379)
                .start();


    }


    public static void beforeEachBase(TestInfo testInfo) {
        var baseProtocol = new Resp3Protocol(FAKE_PORT);
        var proxy = new Resp3Proxy("redis://" + redisImage.getHost() + ":" + redisImage.getPort(), null, null);
        StorageRepository storage = new NullStorageRepository();
        if (testInfo != null && testInfo.getTestClass().isPresent() &&
                testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();

            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                storage = new FileStorageRepository(Path.of("target", "tests", className, method, dsp));
            } else {
                storage = new FileStorageRepository(Path.of("target", "tests", className, method));
            }
        }
        storage.initialize();
        var pl = new RedisRecordingPlugin();
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {

        protocolServer.stop();
    }

    public static void afterClassBase() throws Exception {
        redisImage.close();
    }
}
