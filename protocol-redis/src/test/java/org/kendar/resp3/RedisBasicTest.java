package org.kendar.resp3;


import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.redis.Resp3Protocol;
import org.kendar.redis.Resp3Proxy;
import org.kendar.redis.plugins.RedisLatencyPlugin;
import org.kendar.redis.plugins.RedisNetErrorPlugin;
import org.kendar.redis.plugins.RedisRecordPlugin;
import org.kendar.redis.plugins.RedisReportPlugin;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.tcpserver.TcpServer;
import org.kendar.tests.testcontainer.images.RedisImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.kendar.utils.parser.SimpleParser;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RedisBasicTest {

    protected static final int FAKE_PORT = 6389;
    protected static RedisImage redisImage;
    protected static Server protocolServer;
    protected static ProtocolPluginDescriptor errorPlugin;
    private static final ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    private static ProtocolPluginDescriptor latencyPlugin;

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
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var mapper = new JsonMapper();
        var pl = new RedisRecordPlugin(mapper, storage, new MultiTemplateEngine(), new SimpleParser()).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncRecordPluginSettings());

        var rep = new RedisReportPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        errorPlugin = new RedisNetErrorPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new NetworkErrorPluginSettings().withPercentAction(100));
        latencyPlugin = new RedisLatencyPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new LatencyPluginSettings().withMinMax(500, 1000).withPercentAction(100));

        rep.setActive(true);
        proxy.setPluginHandlers(List.of(pl, rep, errorPlugin, latencyPlugin));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        protocolServer = new NettyServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {
        EventsQueue.unregister("recorder", ReportDataEvent.class);
        events.clear();
        protocolServer.stop();
    }

    public static void afterClassBase() throws Exception {
        redisImage.close();
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }
}
