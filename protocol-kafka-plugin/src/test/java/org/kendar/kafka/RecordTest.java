package org.kendar.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the record plugin persists Kafka interactions (M3 record half): produce
 * through the proxy with recording on, then assert scenario files were written.
 */
public class RecordTest extends KafkaBasicTest {

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo, true);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void recordsProduceTraffic(TestInfo testInfo) throws Exception {
        var topic = "rec-" + UUID.randomUUID().toString().substring(0, 8);
        try (Admin admin = Admin.create(adminProps())) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        }
        try (var producer = new KafkaProducer<String, String>(producerProps())) {
            for (int i = 0; i < 3; i++) {
                producer.send(new ProducerRecord<>(topic, "k" + i, "v" + i)).get();
            }
            producer.flush();
        }

        var dir = Path.of("target", "tests", "RecordTest", "recordsProduceTraffic");
        assertTrue(Files.exists(dir), "recording dir should exist: " + dir);
        try (var files = Files.list(dir)) {
            assertTrue(files.findAny().isPresent(), "recording dir should contain files");
        }
    }

    private Properties adminProps() {
        var p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, proxyBootstrap());
        p.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 20000);
        return p;
    }

    private Properties producerProps() {
        var p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, proxyBootstrap());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        p.put(ProducerConfig.ACKS_CONFIG, "1");
        return p;
    }
}
