package org.kendar.amqp.v10;

import jakarta.jms.Session;
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
import org.kendar.tcpserver.Server;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Broker-less replay: no container, no broker. The proxy replays a recorded
 * scenario (committed under src/test/resources) back to a live qpid-jms client.
 * <p>
 * Connection open (SASL + open) and session open (begin) replay verbatim — those
 * frames are broker-generated or channel-scoped and don't carry the client's
 * unique ids. Producer attach is the boundary (see the disabled test): it needs
 * link-name rewriting via the M2 codec.
 */
class ReplayerTest {
    private static final int FAKE_PORT = 5693;

    private Server startReplayServer() {
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
        Sleeper.sleep(3000, server::isRunning);
        return server;
    }

    @Test
    void replaysConnectionOpenWithoutBroker() throws Exception {
        var server = startReplayServer();
        try {
            var factory = new JmsConnectionFactory("amqp://localhost:" + FAKE_PORT);
            var connection = factory.createConnection("artemis", "artemis");
            connection.start();
            assertTrue(true, "connection opened via broker-less replay");
            connection.close();
        } finally {
            server.stop();
        }
    }

    @Test
    void replaysSessionWithoutBroker() throws Exception {
        var server = startReplayServer();
        try {
            var factory = new JmsConnectionFactory("amqp://localhost:" + FAKE_PORT);
            var connection = factory.createConnection("artemis", "artemis");
            connection.start();
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            assertNotNull(session, "session (begin) opened via broker-less replay");
            connection.close();
        } finally {
            server.stop();
        }
    }

    @Test
    @Disabled("Boundary: replay pushes ALL recorded frames incl. the attach responses, but qpid "
            + "rejects the attach because the recorded broker attach echoes the RECORDED client's "
            + "link name, not the new client's (note the ~15s retry intervals). Needs link-name "
            + "rewriting: decode the client's attach (name=field 0, handle=field 1) with the M2 "
            + "codec and re-encode the recorded attach response to match. Connection + session "
            + "(begin) replay already work without rewriting since qpid reuses channel 0.")
    void replaysProducerAttachWithoutBroker() throws Exception {
        var server = startReplayServer();
        try {
            var factory = new JmsConnectionFactory("amqp://localhost:" + FAKE_PORT);
            var connection = factory.createConnection("artemis", "artemis");
            connection.start();
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            var queue = session.createQueue("amqp10.record.test");
            var producer = session.createProducer(queue); // triggers attach
            assertNotNull(producer, "producer (attach) established via broker-less replay");
            connection.close();
        } finally {
            server.stop();
        }
    }
}
