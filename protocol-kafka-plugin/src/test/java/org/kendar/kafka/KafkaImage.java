package org.kendar.kafka;

import org.kendar.tests.testcontainer.utils.BaseImage;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainer wrapper for a single-node KRaft Kafka broker
 * ({@code apache/kafka-native}, sub-second startup, no ZooKeeper). Testcontainers
 * configures {@code advertised.listeners} to {@code localhost:<mappedPort>}; the
 * proxy connects to {@link #getBootstrapServers()} and the Metadata rewrite makes
 * clients see only the proxy (protocol-kafka.md §8).
 */
public class KafkaImage extends BaseImage<KafkaImage, KafkaContainer> {
    private String bootstrapServers;

    @Override
    protected void preStart() {
        // The JVM image (has /bin/sh) — testcontainers' KafkaContainer injects a shell
        // startup script, which the distroless apache/kafka-native image cannot run.
        container = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));
    }

    @Override
    protected void postStart() {
        bootstrapServers = container.getBootstrapServers();
    }

    /** Raw bootstrap as testcontainers reports it, e.g. {@code localhost:32773}. */
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    /**
     * URI-parseable connection string for the proxy. The Apache KafkaContainer
     * returns {@code host:port} with no scheme, but {@code NetworkProxy} parses the
     * connection string with {@code new URI(...)}, so a scheme is required.
     */
    public String getConnectionString() {
        return bootstrapServers.contains("://") ? bootstrapServers : "tcp://" + bootstrapServers;
    }
}
