package org.kendar.amqp.v09;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class SimpleTest extends BasicTest {

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
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
    void openTest() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.enableHostnameVerification();
        var cs = "amqp://localhost:"+FAKE_PORT;//rabbitContainer.getConnectionString();
        connectionFactory.setUri(cs);
        connectionFactory.setPassword(rabbitContainer.getAdminPassword());
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
        channel.close();
        connection.close();
    }
}
