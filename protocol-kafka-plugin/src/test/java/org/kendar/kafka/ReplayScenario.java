package org.kendar.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * The deterministic client sequence behind the committed replay fixture
 * ({@code src/test/resources/replay_produce_consume}): create a fixed topic,
 * produce one known message, consume it via {@code assign+seekToBeginning}
 * (no consumer group — avoids the coordinator/join/sync chatter). The recorder
 * and the broker-less replay test MUST run this exact sequence so the
 * order/tag-based matching stays aligned; change it and the fixture must be
 * re-recorded ({@code RecordTest#recordReplayFixture}).
 */
final class ReplayScenario {
    static final String TOPIC = "kafka-replay-topic";
    static final String KEY = "replay-key";
    static final String VALUE = "replay-value";

    private ReplayScenario() {
    }

    /** Runs the whole sequence and returns the first consumed record. */
    static ConsumerRecord<String, String> run(String bootstrap) throws Exception {
        try (Admin admin = Admin.create(adminProps(bootstrap))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1))).all().get();
        }
        try (var producer = new KafkaProducer<String, String>(producerProps(bootstrap))) {
            producer.send(new ProducerRecord<>(TOPIC, KEY, VALUE)).get();
            producer.flush();
        }
        try (var consumer = new KafkaConsumer<String, String>(consumerProps(bootstrap))) {
            var tp = new TopicPartition(TOPIC, 0);
            consumer.assign(List.of(tp));
            consumer.seekToBeginning(List.of(tp));
            var deadline = System.currentTimeMillis() + 30000;
            while (System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }
        return null;
    }

    private static Properties adminProps(String bootstrap) {
        var p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 20000);
        p.put(AdminClientConfig.RETRIES_CONFIG, 1);
        return p;
    }

    private static Properties producerProps(String bootstrap) {
        var p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        p.put(ProducerConfig.ACKS_CONFIG, "1");
        p.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 20000);
        return p;
    }

    private static Properties consumerProps(String bootstrap) {
        var p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        return p;
    }
}
