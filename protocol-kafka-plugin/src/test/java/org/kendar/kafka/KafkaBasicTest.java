package org.kendar.kafka;

import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.kafka.plugins.KafkaRecordPlugin;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.kendar.utils.parser.SimpleParser;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Container + proxy scaffold for the Kafka tests. Starts an apache/kafka-native
 * broker, then a {@link KafkaProtocol} proxy in front of it on {@link #FAKE_PORT}
 * (9192 — 9093 is Kafka's conventional controller/TLS port). Manual wiring, no DI,
 * mirroring the AMQP scaffolds.
 */
public class KafkaBasicTest {
    protected static final int FAKE_PORT = 9192;
    protected static final Logger log = LoggerFactory.getLogger(KafkaBasicTest.class);
    protected static KafkaImage kafkaContainer;
    protected static Server protocolServer;
    protected static ProtocolPluginDescriptor recordPlugin;
    protected static StorageRepository storage;
    protected JsonMapper mapper = new JsonMapper();

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        kafkaContainer = new KafkaImage();
        kafkaContainer.start();
    }

    public static void beforeEachBase(TestInfo testInfo) {
        beforeEachBase(testInfo, false);
    }

    public static void beforeEachBase(TestInfo testInfo, boolean record) {
        var baseProtocol = new KafkaProtocol(FAKE_PORT);
        var proxy = new KafkaProxy(kafkaContainer.getConnectionString(), null, null);

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
        recordPlugin = new KafkaRecordPlugin(jsonMapper, storage, new MultiTemplateEngine(), new SimpleParser())
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncRecordPluginSettings());

        var handlers = new ArrayList<BasePluginDescriptor>();
        if (record) {
            handlers.add(recordPlugin);
        }
        proxy.setPluginHandlers(handlers);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();

        protocolServer = new NettyServer(baseProtocol);
        protocolServer.start();
        if (record) {
            recordPlugin.setActive(true);
        }
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {
        if (protocolServer != null) {
            protocolServer.stop();
        }
    }

    public static void afterClassBase() throws Exception {
        if (kafkaContainer != null) {
            kafkaContainer.close();
        }
    }

    protected static String proxyBootstrap() {
        return "localhost:" + FAKE_PORT;
    }
}
