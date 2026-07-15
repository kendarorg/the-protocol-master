package org.kendar.tests.testcontainer.images;

import org.kendar.tests.testcontainer.utils.BaseImage;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * ActiveMQ Artemis — a native AMQP 1.0 broker — for the protocol-amqp-10 tests.
 * Modeled on {@link RabbitMqImage}; {@link RabbitMqImage} is left untouched for
 * the v09 tests.
 */
public class ArtemisImage extends BaseImage<ArtemisImage, GenericContainer<?>> {
    public static final String USER = "artemis";
    public static final String PASSWORD = "artemis";
    private static final int AMQP_PORT = 5672;

    private String host;
    private int port;

    public ArtemisImage() {
        this.withExposedPorts(AMQP_PORT);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /** URI form parsed by {@code NetworkProxy} ({@code uri.getHost()/getPort()}). */
    public String getConnectionString() {
        return "amqp://" + host + ":" + port;
    }

    public String getUserId() {
        return USER;
    }

    public String getPassword() {
        return PASSWORD;
    }

    @Override
    @SuppressWarnings("resource")
    protected void preStart() {
        container = new GenericContainer<>(DockerImageName.parse("apache/activemq-artemis:latest-alpine"))
                .withEnv("ARTEMIS_USER", USER)
                .withEnv("ARTEMIS_PASSWORD", PASSWORD)
                .withEnv("ANONYMOUS_LOGIN", "false");
    }

    @Override
    protected void postStart() {
        host = container.getHost();
        port = container.getMappedPort(AMQP_PORT);
    }
}
