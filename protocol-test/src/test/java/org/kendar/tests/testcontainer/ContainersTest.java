package org.kendar.tests.testcontainer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kendar.tests.testcontainer.images.*;
import org.kendar.tests.testcontainer.utils.Utils;
import org.testcontainers.containers.Network;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ContainersTest {
    @Test
    @Disabled("Only callable directly")
    void testMysql() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var mySqlContainer = new MysqlImage()) {
            mySqlContainer
                    .withNetwork(network)
                    .withAliases("mysql.sample.test", "mysql.proxy.test")
                    .withInitScript("test", "mysql.sql")
                    .start();
            var connection = DriverManager.getConnection(mySqlContainer.getJdbcUrl(),
                    mySqlContainer.getUserId(), mySqlContainer.getPassword());
            var statement = connection.createStatement();
            var resultset = statement.executeQuery("SELECT DENOMINATION FROM COMPANY");
            while (resultset.next()) {
                assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            }
            resultset.close();
            statement.close();
            connection.close();

        }
    }


    @Test
    @Disabled("Only callable directly")
    void testPostgres() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var postgresContainer = new PostgreslImage()) {
            postgresContainer
                    .withNetwork(network)
                    .withAliases("postgres.sample.test", "postgres.proxy.test")
                    .withInitScript("test", "postgres.sql")
                    .start();
            var connection = DriverManager.getConnection(postgresContainer.getJdbcUrl(),
                    postgresContainer.getUserId(), postgresContainer.getPassword());
            var statement = connection.createStatement();
            var resultset = statement.executeQuery("SELECT DENOMINATION FROM COMPANY");
            while (resultset.next()) {
                assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            }
            resultset.close();
            statement.close();
            connection.close();

        }
    }

    @Test
    @Disabled("Only callable directly")
    void testRabbitMq() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var rabbitContainer = new RabbitMqImage()) {
            rabbitContainer
                    .withNetwork(network)
                    .withAliases("rabbitmq.sample.test", "rabbitmq.proxy.test")
                    .start();
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.enableHostnameVerification();
            connectionFactory.setUri(rabbitContainer.getConnectionString());
            connectionFactory.setPassword(rabbitContainer.getAdminPassword());
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection
                    .openChannel()
                    .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
            channel.close();
            connection.close();

        }
    }

    @Test
    @Disabled("Only callable directly")
    void testRedis() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var redisImage = new RedisImage()) {
            redisImage.withNetwork(network)
                    .withAliases("redis.sample.test", "redis.proxy.test")
                    .start();
            JedisPool pool = new JedisPool(redisImage.getHost(), redisImage.getPort());

            try (Jedis jedis = pool.getResource()) {
                // Store & Retrieve a simple string
                jedis.set("foo", "bar");
                assertEquals("bar", jedis.get("foo").toString());

                // Store & Retrieve a HashMap
                Map<String, String> hash = new HashMap<>();
                hash.put("name", "John");
                hash.put("surname", "Smith");
                hash.put("company", "Redis");
                hash.put("age", "29");
                jedis.hset("user-session:123", hash);
                assertEquals("{name=John, surname=Smith, company=Redis, age=29}", jedis.hgetAll("user-session:123").toString());
            }
        }
    }

    @Test
    @Disabled("Only callable directly")
    void testJava() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var javaImage = new JavaImage()) {
            javaImage
                    .withDir("/test")
                    .withFile(Path.of("..", "protocol-runner", "target", "protocol-runner.jar").toString(), "/test/protocol-runner.jar")
                    .withCmd(Path.of("..", "protocol-test", "src", "test", "resources", "run.sh").toString(), "/test/run.sh")
                    .withNetwork(network)
                    .withAliases("java.sample.test")
                    .start();
            Thread.sleep(2000);
            var logs = javaImage.getLogs();
            assertTrue(logs.contains("protocol-runner.jar"));
            assertTrue(logs.contains("run.sh"));

        }
    }
}
