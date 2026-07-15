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
import org.kendar.kafka.plugins.KafkaLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises a proxy-side plugin (latency) on the Kafka path: the injected delay
 * must slow but not break the produce/consume round-trip through the proxy.
 */
public class SpecialErrorsTest extends KafkaBasicTest {

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
        var latency = new KafkaLatencyPlugin(new JsonMapper())
                .initialize(new GlobalSettings(), new ByteProtocolSettingsWithLogin(),
                        new LatencyPluginSettings().withMinMax(50, 120).withPercentAction(100));
        extraPlugins.add(latency);
        beforeEachBase(testInfo, false);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
        extraPlugins.clear();
    }

    @Test
    void latencyDoesNotBreakRoundTrip() throws Exception {
        var topic = "lat-" + UUID.randomUUID().toString().substring(0, 8);
        var messages = 3;

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
                consumed += consumer.poll(Duration.ofMillis(500)).count();
            }
        }
        assertEquals(messages, consumed, "message did not arrive through the latency plugin");
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
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "lat-group-" + UUID.randomUUID());
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        return p;
    }
}
