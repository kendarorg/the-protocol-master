package org.kendar.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.kafka.plugins.KafkaReplayPlugin;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.settings.BasicAysncRecordPluginSettings;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.kafka.plugins.KafkaRecordPlugin;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.kendar.utils.parser.SimpleParser;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3 gate: record a deterministic AdminClient {@code describeCluster} through the
 * proxy, then replay it <b>broker-less</b> (no container connection) and assert the
 * client still gets the cluster nodes purely from the recording.
 */
public class ReplayerTest extends KafkaBasicTest {

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    @Test
    void replaysDescribeClusterWithoutBroker() throws Exception {
        var recordDir = Path.of("target", "tests", "ReplayerTest", "replay");

        // --- phase A: record describeCluster through the real broker ---
        recordDescribeCluster(recordDir);

        // --- phase B: replay broker-less ---
        var server = startReplayServer(recordDir);
        try {
            try (Admin admin = Admin.create(adminProps())) {
                var nodes = admin.describeCluster().nodes().get();
                assertFalse(nodes.isEmpty(), "replay should return cluster nodes with no broker");
            }
        } finally {
            server.stop();
        }
    }

    private void recordDescribeCluster(Path recordDir) {
        var proto = new KafkaProtocol(FAKE_PORT);
        var proxy = new KafkaProxy(kafkaContainer.getConnectionString(), null, null);
        var st = new FileStorageRepository(recordDir);
        st.initialize();
        var gs = new GlobalSettings();
        var record = new KafkaRecordPlugin(new JsonMapper(), st, new MultiTemplateEngine(), new SimpleParser())
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncRecordPluginSettings());
        proxy.setPluginHandlers(List.<BasePluginDescriptor>of(record));
        proto.setProxy(proxy);
        proto.initialize();
        Server server = new NettyServer(proto);
        server.start();
        record.setActive(true);
        Sleeper.sleep(3000, server::isRunning);
        try (Admin admin = Admin.create(adminProps())) {
            admin.describeCluster().nodes().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Sleeper.sleep(500);
            server.stop();
        }
    }

    private Server startReplayServer(Path recordDir) {
        var proto = new KafkaProtocol(FAKE_PORT);
        var proxy = new KafkaProxy(); // no broker
        var st = new FileStorageRepository(recordDir);
        st.initialize();
        var gs = new GlobalSettings();
        var replay = new KafkaReplayPlugin(new JsonMapper(), st)
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.<BasePluginDescriptor>of(replay));
        proto.setProxy(proxy);
        proto.initialize();
        Server server = new NettyServer(proto);
        server.start();
        replay.setActive(true);
        Sleeper.sleep(3000, server::isRunning);
        return server;
    }

    private Properties adminProps() {
        var p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, proxyBootstrap());
        p.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 20000);
        p.put(AdminClientConfig.RETRIES_CONFIG, 1);
        p.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, 600000);
        return p;
    }
}
