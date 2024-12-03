package org.kendar.amqp.v09;


import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.TestInfo;
import org.kendar.amqp.v09.plugins.AmqpRecordPlugin;
import org.kendar.amqp.v09.plugins.AmqpReportPlugin;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tests.testcontainer.images.RabbitMqImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 5682;
    protected static RabbitMqImage rabbitContainer;
    protected static TcpServer protocolServer;
    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();

    public static void beforeClassBase() {
        //LoggerBuilder.setLevel(Logger.ROOT_LOGGER_NAME, Level.DEBUG);

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
        gs.putService("storage", storage);
        var pl = new AmqpRecordPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());
        var rep = new AmqpReportPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        rep.setActive(true);
        proxy.setPlugins(List.of(
                pl, rep));
        pl.setActive(true);
        rep.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
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
        return events.stream().collect(Collectors.toList());
    }
}
