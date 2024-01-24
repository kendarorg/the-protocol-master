package org.kendar.testcontainer.images;

import org.kendar.testcontainer.utils.BaseImage;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public class MongoDbImage extends BaseImage<MongoDbImage, MongoDBContainer> {
    private String connectionString;
    private String userId;
    private String password;
    public MongoDbImage() {
        this.withExposedPorts(27017);
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

        container = new MongoDBContainer(DockerImageName.parse("mongo:4.4.2"));
    }

    @Override
    protected void postStart() {
        connectionString = container.getConnectionString();
        userId = "root";
        password = "test";
    }
}
