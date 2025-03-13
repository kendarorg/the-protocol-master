package org.kendar.amqp.v09;

import com.rabbitmq.client.*;
import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SpecialErrorsTest extends AmqpBasicTest {

    public static final String MAIN_QUEUE = "fuffa_queue";
    public static final String DEFAULT_MESSAGE_CONTENT = "zzzz details";

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
    }

    private static Channel consume(ConnectionFactory connectionFactory, ConcurrentHashMap<Integer, String> messages, Channel... channels) throws IOException, TimeoutException {
        Channel channelConsume = null;
        if (channels.length == 1) {
            channelConsume = channels[0];

        } else {
            var connectionConsume = connectionFactory.newConnection();
            channelConsume = connectionConsume
                    .openChannel()
                    .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
            channelConsume.queueDeclare(MAIN_QUEUE, false, false, false, null);
        }

        var channelBlocked = channelConsume;
        AtomicInteger resultMessage = new AtomicInteger(1);
        DefaultConsumer consumer = new DefaultConsumer(channelConsume) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                String message = new String(body, StandardCharsets.UTF_8);
                messages.put(resultMessage.getAndIncrement(), message);
                channelBlocked.basicAck(envelope.getDeliveryTag(), false);
                // process the message
            }
        };
        channelBlocked.basicConsume(MAIN_QUEUE, false, consumer);
        return channelBlocked;
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
    void randomErrors() throws Exception {
        errorPlugin.setActive(true);
        assertThrows(Exception.class, () -> {
            var messages = new ConcurrentHashMap<Integer, String>();
            String exectedMessage = DEFAULT_MESSAGE_CONTENT;
            ConnectionFactory connectionFactory = new ConnectionFactory();
            var cs = "amqp://localhost:" + FAKE_PORT;
            //cs = rabbitContainer.getConnectionString();
            Sleeper.sleep(100);

            connectionFactory.setUri(cs);
            connectionFactory.setPassword(rabbitContainer.getAdminPassword());

            Connection connection = connectionFactory.newConnection();
            Channel channel = connection
                    .openChannel()
                    .orElseThrow(() -> new RuntimeException("Failed to Open channel"));


            channel.queueDeclare(MAIN_QUEUE, false, false, false, null);

            var chanConsume = consume(connectionFactory, messages, channel);


            Sleeper.sleep(100);


            var props = new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(1)
                    .appId("TESTAPP")
                    .build();
            Sleeper.sleep(100);
            channel.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "1").getBytes());
            Sleeper.sleep(100);
            channel.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "2").getBytes());
            chanConsume.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "3").getBytes());
            Sleeper.sleep(100);

            System.out.println("QEUEUE DELETE------------------------------------------------------");
            channel.queueDelete(MAIN_QUEUE);

            Sleeper.sleep(100);
            System.out.println("CLOSE CHANNEL ------------------------------------------------");
            channel.close();
            Sleeper.sleep(100);
            System.out.println("CLOSE CONNNECTION ------------------------------------------------");
            connection.close();


            Sleeper.sleep(1000, () -> messages.size() == 3);

            assertEquals(3, messages.size());
            assertTrue(messages.containsValue(exectedMessage + "1"));
            assertTrue(messages.containsValue(exectedMessage + "2"));
            assertTrue(messages.containsValue(exectedMessage + "3"));
        });
        errorPlugin.setActive(false);
    }
}
