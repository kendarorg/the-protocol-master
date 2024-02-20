package org.kendar.testcontainer.images;

import org.kendar.testcontainer.utils.BaseImage;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public class RabbitMqImage extends BaseImage<RabbitMqImage, RabbitMQContainer> {
    private String connectionString;
    private String userId;
    private String password;
    private String adminPassword;
    private String adminUserId;

    public RabbitMqImage() {
        this.withExposedPorts(5672);
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    @Override
    protected void preStart() {

        container = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"));
    }

    @Override
    protected void postStart() {
        connectionString = container.getAmqpUrl();
        userId = "guest";
        password = "guest";
        adminPassword = container.getAdminPassword();
        adminUserId = container.getAdminUsername();
    }
}
