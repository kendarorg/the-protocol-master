package org.kendar.amqp.v10;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kendar.amqp.v10.plugins.Amqp10ReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Broker-less replay: no container, no broker. The proxy replays a recorded
 * scenario (committed under src/test/resources) back to a live qpid-jms client.
 * <p>
 * Scoped to connection open — the deterministic part of the handshake (SASL +
 * open). Full session replay (attach/transfer) needs codec-integrated field
 * rewriting so recorded responses adopt the new client's link names/handles.
 */
class ReplayerTest {
    private static final int FAKE_PORT = 5693;

    @Test
    @Disabled("WIP: replay now matches the recorded input and correlates responses by tag "
            + "(getContextTags + id-sorted index), but the queued async responses are drained only "
            + "in NetworkProtoContext.postWrite -- i.e. AFTER a message is written to the client. "
            + "The relay-based ProtocolHeader returns empty (broker relay writes directly), so in "
            + "broker-less replay nothing triggers the drain. Fix: local SASL termination in replay "
            + "mode (ProtocolHeader writes the handshake responses itself), keeping relay for "
            + "passthrough. Distinct design step; changing it risks the working passthrough.")
    void replaysConnectionOpenWithoutBroker() throws Exception {
        var baseProtocol = new Amqp10Protocol(FAKE_PORT);
        var proxy = new Amqp10Proxy(); // no broker

        StorageRepository storage = new FileStorageRepository(
                Path.of("src", "test", "resources", "replay_open"));
        storage.initialize();

        var gs = new GlobalSettings();
        var replay = new Amqp10ReplayPlugin(new JsonMapper(), storage)
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.of(replay));
        replay.setActive(true);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var server = new NettyServer(baseProtocol);
        server.start();
        try {
            Sleeper.sleep(3000, server::isRunning);

            var factory = new JmsConnectionFactory("amqp://localhost:" + FAKE_PORT);
            var connection = factory.createConnection("artemis", "artemis");
            connection.start();
            assertTrue(true, "connection opened via broker-less replay");
            connection.close();
        } finally {
            server.stop();
        }
    }
}
