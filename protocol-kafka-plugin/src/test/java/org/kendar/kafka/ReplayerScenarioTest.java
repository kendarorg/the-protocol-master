package org.kendar.kafka;

import org.junit.jupiter.api.Test;
import org.kendar.kafka.plugins.KafkaReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Broker-less replay from the COMMITTED scenario (no container, no Docker): the
 * proxy answers a live kafka-clients Admin/producer/consumer purely from
 * {@code src/test/resources/replay_produce_consume}, and the consumed message
 * content is verified against the recorded one. This is the regression oracle
 * the record/replay pair is measured against — mirrors amqp-10's ReplayerTest.
 * <p>
 * The fixture pins the recorded api versions, so upgrading the kafka-clients
 * dependency requires re-recording ({@code RecordTest#recordReplayFixture}).
 */
class ReplayerScenarioTest {
    private static final int FAKE_PORT = 9192;

    @Test
    void replaysProduceAndConsumeFromCommittedScenario() throws Exception {
        var proto = new KafkaProtocol(FAKE_PORT);
        var proxy = new KafkaProxy(); // no broker

        var storage = new FileStorageRepository(
                Path.of("src", "test", "resources", "replay_produce_consume"));
        storage.initialize();

        var gs = new GlobalSettings();
        var replay = new KafkaReplayPlugin(new JsonMapper(), storage)
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.of(replay));
        replay.setActive(true);

        proto.setProxy(proxy);
        proto.initialize();
        Server server = new NettyServer(proto);
        server.start();
        Sleeper.sleep(3000, server::isRunning);
        try {
            var received = ReplayScenario.run("localhost:" + FAKE_PORT);
            assertNotNull(received, "no message consumed from broker-less replay");
            assertEquals(ReplayScenario.KEY, received.key(), "replayed message key mismatch");
            assertEquals(ReplayScenario.VALUE, received.value(), "replayed message value mismatch");
        } finally {
            server.stop();
        }
    }
}
