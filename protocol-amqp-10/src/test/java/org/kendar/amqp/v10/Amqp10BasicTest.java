package org.kendar.amqp.v10;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.TestInfo;
import org.kendar.amqp.v10.plugins.Amqp10RecordPlugin;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.tests.testcontainer.images.ArtemisImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.kendar.utils.parser.SimpleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Container + proxy scaffold for the AMQP 1.0 tests (M3 gate). Starts ActiveMQ
 * Artemis, then an {@link Amqp10Protocol} proxy in front of it on {@link #FAKE_PORT}.
 * Pattern mirrors the v09 {@code AmqpBasicTest} (manual wiring, no DI).
 */
public class Amqp10BasicTest {
    protected static final int FAKE_PORT = 5692;
    protected static final Logger log = LoggerFactory.getLogger(Amqp10BasicTest.class);
    protected static ArtemisImage artemisContainer;
    protected static Server protocolServer;
    protected static ProtocolPluginDescriptor recordPlugin;
    protected static StorageRepository storage;
    /** Extra active plugins (e.g. latency/net-error) added by a subclass before beforeEach. */
    protected static final java.util.List<ProtocolPluginDescriptor> extraPlugins = new java.util.ArrayList<>();
    protected JsonMapper mapper = new JsonMapper();

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        artemisContainer = new ArtemisImage();
        artemisContainer
                .withNetwork(network)
                .waitingForPort(5672)
                .start();

        Sleeper.sleep(60000, () -> {
            try {
                var factory = new JmsConnectionFactory(artemisContainer.getConnectionString());
                var connection = factory.createConnection(
                        artemisContainer.getUserId(), artemisContainer.getPassword());
                connection.start();
                connection.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static void beforeEachBase(TestInfo testInfo) {
        beforeEachBase(testInfo, false);
    }

    public static void beforeEachBase(TestInfo testInfo, boolean record) {
        var baseProtocol = new Amqp10Protocol(FAKE_PORT);
        var proxy = new Amqp10Proxy(artemisContainer.getConnectionString(),
                artemisContainer.getUserId(), artemisContainer.getPassword());

        if (record && testInfo != null && testInfo.getTestClass().isPresent()
                && testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            storage = new FileStorageRepository(Path.of("target", "tests", className, method));
        } else {
            storage = new NullStorageRepository();
        }
        storage.initialize();

        var gs = new GlobalSettings();
        var jsonMapper = new JsonMapper();
        recordPlugin = new Amqp10RecordPlugin(jsonMapper, storage, new MultiTemplateEngine(), new SimpleParser())
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncRecordPluginSettings());

        var handlers = new java.util.ArrayList<org.kendar.plugins.base.BasePluginDescriptor>();
        if (record) {
            handlers.add(recordPlugin);
        }
        handlers.addAll(extraPlugins);
        proxy.setPluginHandlers(handlers);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();

        protocolServer = new NettyServer(baseProtocol);
        protocolServer.start();
        if (record) {
            recordPlugin.setActive(true);
        }
        for (var p : extraPlugins) {
            p.setActive(true);
        }
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {
        if (protocolServer != null) {
            protocolServer.stop();
        }
    }

    public static void afterClassBase() throws Exception {
        if (artemisContainer != null) {
            artemisContainer.close();
        }
    }

    /** qpid-jms factory pointed at the proxy (not the broker directly). */
    protected static JmsConnectionFactory proxyFactory() {
        return new JmsConnectionFactory("amqp://localhost:" + FAKE_PORT);
    }
}
