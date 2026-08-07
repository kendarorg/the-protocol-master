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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end passthrough: a qpid-jms producer/consumer talks AMQP 1.0 through the
 * proxy to Artemis. This is the M1 verification gate.
 */
class SimpleTest extends Amqp10BasicTest {

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
        beforeEachBase(testInfo);
    }

    @AfterEach
    void afterEach() {
        afterEachBase();
    }

    @Test
    void produceAndConsumeThroughProxy() throws Exception {
        var factory = proxyFactory();
        try (var connection = factory.createConnection(
                artemisContainer.getUserId(), artemisContainer.getPassword())) {
            connection.start();
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            var queue = session.createQueue("amqp10.simple.test");

            var producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(session.createTextMessage("hello-amqp10"));

            var consumer = session.createConsumer(queue);
            var received = (TextMessage) consumer.receive(10000);

            assertNotNull(received, "no message received through the proxy");
            assertEquals("hello-amqp10", received.getText());
        }
    }
}
