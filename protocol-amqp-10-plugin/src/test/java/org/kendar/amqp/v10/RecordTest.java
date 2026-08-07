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
import org.kendar.utils.Sleeper;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Records a produce/consume session through the proxy and asserts the recorder
 * wrote a scenario (numbered JSON interaction files). M3 record gate.
 */
class RecordTest extends Amqp10BasicTest {

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
        beforeEachBase(testInfo, true);
    }

    @AfterEach
    void afterEach() {
        afterEachBase();
    }

    @Test
    void recordsAScenario() throws Exception {
        var factory = proxyFactory();
        try (var connection = factory.createConnection(
                artemisContainer.getUserId(), artemisContainer.getPassword())) {
            connection.start();
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            var queue = session.createQueue("amqp10.record.test");

            var producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(session.createTextMessage("record-me"));

            var consumer = session.createConsumer(queue);
            var received = (TextMessage) consumer.receive(10000);
            assertNotNull(received, "no message received through the proxy");
        }

        // recordInteraction writes json files asynchronously; give it a moment
        var scenarioDir = Path.of("target", "tests", "RecordTest", "recordsAScenario", "scenario").toFile();
        Sleeper.sleep(2000, () -> hasJsonFiles(scenarioDir));
        assertTrue(hasJsonFiles(scenarioDir),
                "expected recorded scenario json files under " + scenarioDir);
    }

    private static boolean hasJsonFiles(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        var files = dir.list((d, name) -> name.endsWith(".json"));
        return files != null && files.length > 0;
    }
}
