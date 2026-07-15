package org.kendar.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M1 gate: AdminClient createTopics, produce N and consume N — all through the
 * proxy (Metadata/FindCoordinator rewrite + ApiVersions cap + produce/fetch
 * passthrough). The client only ever talks to the proxy on {@link #FAKE_PORT}.
 */
public class SimpleTest extends KafkaBasicTest {

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
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void produceAndConsumeThroughProxy() throws Exception {
        var topic = "tpm-topic-" + UUID.randomUUID().toString().substring(0, 8);
        var messages = 5;

        try (Admin admin = Admin.create(adminProps())) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        }

        try (var producer = new KafkaProducer<String, String>(producerProps())) {
            for (int i = 0; i < messages; i++) {
                producer.send(new ProducerRecord<>(topic, "k" + i, "v" + i)).get();
            }
            producer.flush();
        }

        int consumed = 0;
        try (var consumer = new KafkaConsumer<String, String>(consumerProps())) {
            consumer.subscribe(List.of(topic));
            var deadline = System.currentTimeMillis() + 30000;
            while (consumed < messages && System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                consumed += records.count();
            }
        }

        assertEquals(messages, consumed);
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

    private Properties consumerProps() {
        var p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, proxyBootstrap());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "tpm-group-" + UUID.randomUUID());
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        return p;
    }
}
