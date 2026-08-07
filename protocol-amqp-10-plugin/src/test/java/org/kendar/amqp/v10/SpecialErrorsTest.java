package org.kendar.amqp.v10;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kendar.amqp.v10.plugins.Amqp10LatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises a proxy-side plugin (latency) on the AMQP 1.0 path: the injected delay
 * must not break the round-trip. M4 plugin gate.
 */
class SpecialErrorsTest extends Amqp10BasicTest {

    @BeforeAll
    static void beforeAll() {
        beforeClassBase();
    }

    @AfterAll
    static void afterAll() throws Exception {
        afterClassBase();
    }

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        var latency = new Amqp10LatencyPlugin(new JsonMapper())
                .initialize(new GlobalSettings(), new ByteProtocolSettingsWithLogin(),
                        new LatencyPluginSettings().withMinMax(150, 300).withPercentAction(100));
        extraPlugins.add(latency);
        beforeEachBase(testInfo, false);
    }

    @AfterEach
    void afterEach() {
        afterEachBase();
        extraPlugins.clear();
    }

    @Test
    void latencyDoesNotBreakRoundTrip() throws Exception {
        var factory = proxyFactory();
        try (var connection = factory.createConnection(
                artemisContainer.getUserId(), artemisContainer.getPassword())) {
            connection.start();
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            var queue = session.createQueue("amqp10.latency.test");

            var producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(session.createTextMessage("slow-but-arrives"));

            var consumer = session.createConsumer(queue);
            var received = (TextMessage) consumer.receive(15000);

            assertNotNull(received, "message did not arrive through the latency plugin");
            assertEquals("slow-but-arrives", received.getText());
        }
    }
}
