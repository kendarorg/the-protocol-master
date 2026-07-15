package org.kendar.amqp.v10;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.qpid.jms.JmsConnectionFactory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Broker-less replay: no container, no broker. The proxy replays a recorded
 * scenario (committed under src/test/resources) back to a live qpid-jms client.
 * <p>
 * Connection open (SASL + open) and session open (begin) replay verbatim — those
 * frames are broker-generated or channel-scoped and don't carry the client's
 * unique ids. Producer attach replays via link-name rewriting: the plugin captures
 * the client's attach name and surgically rewrites the recorded attach response.
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

    @Test
    void replaysProducerAndConsumerWithoutBroker() throws Exception {
        // Mirrors the recorded RecordTest sequence so order-based matching stays aligned.
        var server = startReplayServer();
        try {
            var factory = new JmsConnectionFactory("amqp://localhost:" + FAKE_PORT);
            var connection = factory.createConnection("artemis", "artemis");
            connection.start();
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            var queue = session.createQueue("amqp10.record.test");

            var producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(session.createTextMessage("record-me"));

            var consumer = session.createConsumer(queue);
            var received = (TextMessage) consumer.receive(10000);

            assertNotNull(received, "recorded delivery did not reach the consumer via replay");
            assertEquals("record-me", received.getText(), "replayed message body mismatch");
            connection.close();
        } finally {
            server.stop();
        }
    }
}
