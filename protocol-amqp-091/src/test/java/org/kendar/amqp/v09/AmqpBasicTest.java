package org.kendar.amqp.v09;


import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.TestInfo;
import org.kendar.amqp.v09.plugins.*;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.tcpserver.TcpServer;
import org.kendar.tests.testcontainer.images.RabbitMqImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.kendar.utils.parser.SimpleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("rawtypes")
public class AmqpBasicTest {
    protected static final int FAKE_PORT = 5682;
    protected static final Logger log = LoggerFactory.getLogger(FileStorageRepository.class);
    protected static RabbitMqImage rabbitContainer;
    protected static Server protocolServer;
    protected static ProtocolPluginDescriptor publishPlugin;
    protected static ProtocolPluginDescriptor recordPlugin;
    protected static ProtocolPluginDescriptor errorPlugin;
    private static final ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    private static ProtocolPluginDescriptor latencyPlugin;
    protected JsonMapper mapper = new JsonMapper();

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        rabbitContainer = new RabbitMqImage();
        rabbitContainer
                .withNetwork(network)
                .waitingForPort(5672)
                .start();

        Sleeper.sleep(60000, () -> {
            try {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.setUri(rabbitContainer.getConnectionString());
                connectionFactory.setPassword(rabbitContainer.getAdminPassword());
                var connection = connectionFactory.newConnection();
                connection.isOpen();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

    }

    public static void beforeEachBase(TestInfo testInfo) {
        var baseProtocol = new AmqpProtocol(FAKE_PORT);
        var proxy = new AmqpProxy(rabbitContainer.getConnectionString(),
                rabbitContainer.getUserId(), rabbitContainer.getPassword());
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
        var mapper = new JsonMapper();
        recordPlugin = new AmqpRecordPlugin(mapper, storage,
                new MultiTemplateEngine(), new SimpleParser()).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncRecordPluginSettings());
        var rep = new AmqpReportPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        publishPlugin = new AmqpPublishPlugin(mapper, new MultiTemplateEngine()).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        errorPlugin = new AmqpNetErrorPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new NetworkErrorPluginSettings().withPercentAction(80));
        latencyPlugin = new AmqpLatencyPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new LatencyPluginSettings().withMinMax(500, 1000).withPercentAction(100));

        proxy.setPluginHandlers(List.of(
                recordPlugin, rep, publishPlugin, errorPlugin, latencyPlugin));

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);

        protocolServer = new NettyServer(baseProtocol);

        protocolServer.start();

        recordPlugin.setActive(true);
        rep.setActive(true);

        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {
        EventsQueue.unregister("recorder", ReportDataEvent.class);
        events.clear();
        protocolServer.stop();
    }

    public static void afterClassBase() throws Exception {
        rabbitContainer.close();
    }

    public List<ReportDataEvent> getEvents() {
        return new ArrayList<>(events);
    }
}
