package org.kendar.testcontainer.images;

import com.redis.testcontainers.RedisContainer;
import org.kendar.testcontainer.utils.BaseImage;
import org.testcontainers.utility.DockerImageName;

public class RedisImage extends BaseImage<RedisImage, RedisContainer> {


    private String host;
    private Integer port;

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public RedisImage() {
        this.withExposedPorts(6379);
    }


    @Override
    protected void preStart() {

        container = new RedisContainer(DockerImageName.parse("redis:7.2.4-alpine"));

    }

    @Override
    protected void postStart() {
        this.host= container.getHost();
        this.port = container.getMappedPort(6379);
        System.setProperty("spring.redis.host", container.getHost());
        System.setProperty("spring.redis.port", container.getMappedPort(6379).toString());

    }
}
