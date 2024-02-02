package org.kendar.amqp.v09;

import com.rabbitmq.client.*;
import org.junit.jupiter.api.*;
import org.kendar.protocol.Sleeper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void openConnectionTest() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
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

    @Test
    @Disabled("TODO TO IMPLEMENT")
    void queueTest() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        String exectedMessage = "product details";
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.enableHostnameVerification();
        var cs = "amqp://localhost:"+FAKE_PORT;//rabbitContainer.getConnectionString();
        connectionFactory.setUri(cs);
        connectionFactory.setPassword(rabbitContainer.getAdminPassword());

        var connectionConsume = connectionFactory.newConnection();
        var channelConsume = connectionConsume
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
        channelConsume.queueDeclare("products_queue", false, false, false, null);

        AtomicReference<String> resultMessage = new AtomicReference<>();
        resultMessage.set("");
        DefaultConsumer consumer = new DefaultConsumer(channelConsume) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                String message = new String(body, "UTF-8");
                resultMessage.set(message);
                // process the message
            }
        };
        channelConsume.basicConsume("products_queue", true, consumer);


        Sleeper.sleep(100);

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
        channel.queueDeclare("products_queue", false, false, false, null);

        var props = new AMQP.BasicProperties.Builder()
                .appId("TESTAPP")
                .contentType("text/plain")
                .build();
        channel.basicPublish("", "products_queue", props, exectedMessage.getBytes());
        channel.close();
        connection.close();


        Sleeper.sleep(100);

        assertEquals(exectedMessage,resultMessage.get());
    }
}
