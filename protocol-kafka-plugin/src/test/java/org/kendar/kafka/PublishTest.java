package org.kendar.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kendar.kafka.plugins.KafkaPublishPlugin;
import org.kendar.kafka.plugins.apis.KafkaPublishPluginApis;
import org.kendar.kafka.plugins.apis.dtos.PublishKafkaMessage;
import org.kendar.settings.GlobalSettings;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M4 gate: the publish plugin produces a message for real through the proxy's
 * upstream connection (exercising the CRC32C v9 Produce encoder), and a consumer
 * reading through the proxy receives it.
 */
public class PublishTest extends KafkaBasicTest {

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
    void publishThroughProxyIsConsumable() throws Exception {
        var topic = "pub-" + UUID.randomUUID().toString().substring(0, 8);
        try (Admin admin = Admin.create(adminProps())) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        }

        try (var consumer = new KafkaConsumer<String, String>(consumerProps())) {
            consumer.subscribe(List.of(topic));
            // Establish live connections through the proxy (join group, assign).
            for (int i = 0; i < 10; i++) {
                consumer.poll(Duration.ofMillis(300));
            }

            // Build the publish plugin against the running proxy and produce.
            var plugin = new KafkaPublishPlugin(new JsonMapper(), new MultiTemplateEngine());
            plugin.initialize(new GlobalSettings(), baseProtocol.getSettings(),
                    new org.kendar.settings.PluginSettings());
            plugin.setProtocolInstance(baseProtocol);
            var apis = new KafkaPublishPluginApis(plugin, plugin.getId(), plugin.getInstanceId(),
                    new MultiTemplateEngine());

            var msg = new PublishKafkaMessage();
            msg.setTopic(topic);
            msg.setKey("pk");
            msg.setBody("published-through-proxy");
            msg.setAcks((short) 1);
            int produced = apis.doPublish(msg, 0);
            assertTrue(produced > 0, "publish plugin should produce through a live connection");

            String received = null;
            var deadline = System.currentTimeMillis() + 20000;
            while (received == null && System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (var r : records) {
                    if (r.topic().equals(topic)) {
                        received = r.value();
                    }
                }
                Sleeper.sleep(50);
            }
            assertEquals("published-through-proxy", received);
        }
    }

    private Properties adminProps() {
        var p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, proxyBootstrap());
        p.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 20000);
        return p;
    }

    private Properties consumerProps() {
        var p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, proxyBootstrap());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "pub-group-" + UUID.randomUUID());
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        return p;
    }
}
