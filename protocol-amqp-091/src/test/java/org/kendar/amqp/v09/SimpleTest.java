package org.kendar.amqp.v09;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rabbitmq.client.*;
import org.junit.jupiter.api.*;
import org.kendar.amqp.v09.apis.AmqpPublishPluginApis;
import org.kendar.amqp.v09.apis.dtos.AmqpConnection;
import org.kendar.amqp.v09.apis.dtos.PublishAmqpMessage;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleTest extends BasicTest {

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
    void test0_sameChannel() throws Exception {
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
//                .contentEncoding("UTF-8")
                .deliveryMode(1)
//                .priority(2)
//                .correlationId("3") //?
//                //.replyTo("4")
//                //.expiration("5")
//                .messageId("6")
//                .timestamp(new Date())
//                //.type("7")
//                //.userId("8")
                .appId("TESTAPP")
                //.clusterId("9")
                .build();
        //SimpleProxyServer.write=true;
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
    }


    /**
     * PUT IT AS TEST4 to BREAK
     *
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     * @throws TimeoutException
     */
    @Test
    void test4_diffChannelSameConnection() throws Exception {
        var messages = new ConcurrentHashMap<Integer, String>();
        String exectedMessage = DEFAULT_MESSAGE_CONTENT;
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // connectionFactory.enableHostnameVerification();
        var cs = "amqp://localhost:" + FAKE_PORT;
        //var realCs = new URI(rabbitContainer.getConnectionString());
        //cs = rabbitContainer.getConnectionString();
        Sleeper.sleep(100);

        connectionFactory.setUri(cs);
        connectionFactory.setPassword(rabbitContainer.getAdminPassword());

        Connection connection = connectionFactory.newConnection();
        Channel channel1 = connection
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));


        channel1.queueDeclare(MAIN_QUEUE, false, false, false, null);

        var chanConsume = consume(connectionFactory, messages, channel1);

        Channel channel = connection
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));


        channel.queueDeclare(MAIN_QUEUE, false, false, false, null);


        Sleeper.sleep(100);


        var props = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
//                .contentEncoding("UTF-8")
                .deliveryMode(1)
//                .priority(2)
//                .correlationId("3") //?
//                //.replyTo("4")
//                //.expiration("5")
//                .messageId("6")
//                .timestamp(new Date())
//                //.type("7")
//                //.userId("8")
                .appId("TESTAPP")
                //.clusterId("9")
                .build();
        //SimpleProxyServer.write=true;
        Sleeper.sleep(100);
        channel.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "1").getBytes());
        Sleeper.sleep(100);
        channel.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "2").getBytes());
        chanConsume.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "3").getBytes());
        System.out.println("------------------------------------------------------------");


        Sleeper.sleep(1000, () -> {
            System.err.println("SIZE: " + messages.size());
            return messages.size() == 3;
        });
        channel1.queueDelete(MAIN_QUEUE);
        channel.queueDelete(MAIN_QUEUE);
        channel.close();
        connection.close();

        assertEquals(3, messages.size());
        assertTrue(messages.containsValue(exectedMessage + "1"));
        assertTrue(messages.containsValue(exectedMessage + "2"));
        assertTrue(messages.containsValue(exectedMessage + "3"));
    }

    void test5_noPublish() throws Exception {

    }

    @Test
    void test2_differentChannelAndConnection() throws Exception {
        var messages = new ConcurrentHashMap<Integer, String>();
        String exectedMessage = DEFAULT_MESSAGE_CONTENT;

        Sleeper.sleep(5000, () -> protocolServer.isRunning());
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // connectionFactory.enableHostnameVerification();
        var cs = "amqp://localhost:" + FAKE_PORT;
        var realCs = new URI(rabbitContainer.getConnectionString());
        //cs = rabbitContainer.getConnectionString();
        Sleeper.sleep(100);

        connectionFactory.setUri(cs);
        connectionFactory.setPassword(rabbitContainer.getAdminPassword());
        System.out.println("LISTENING ------------------------------------------------------------");

        var chanConsume = consume(connectionFactory, messages);
        System.out.println("PREPARING ------------------------------------------------------------");

        Sleeper.sleep(100);

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
        channel.queueDeclare(MAIN_QUEUE, false, false, false, null);

        var props = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
//                .contentEncoding("UTF-8")
                .deliveryMode(1)
//                .priority(2)
//                .correlationId("3") //?
//                //.replyTo("4")
//                //.expiration("5")
//                .messageId("6")
//                .timestamp(new Date())
//                //.type("7")
//                //.userId("8")
                .appId("TESTAPP")
                //.clusterId("9")
                .build();
        System.out.println("SENDING ------------------------------------------------------------");
        //SimpleProxyServer.write=true;
        Sleeper.sleep(100);
        channel.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "1").getBytes());
        Sleeper.sleep(100);
        channel.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "2").getBytes());
        chanConsume.basicPublish("", MAIN_QUEUE, props, (exectedMessage + "3").getBytes());
        System.out.println("WAIT------------------------------------------------------------");
        Sleeper.sleep(100);

        chanConsume.queueDelete(MAIN_QUEUE);
        channel.queueDelete(MAIN_QUEUE);
        channel.close();
        connection.close();


        Sleeper.sleep(100);

        assertEquals(3, messages.size());
        assertTrue(messages.containsValue(exectedMessage + "1"));
        assertTrue(messages.containsValue(exectedMessage + "2"));
        assertTrue(messages.containsValue(exectedMessage + "3"));
    }


    @Test
    void test3_openConnection() throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.enableHostnameVerification();
        var cs = "amqp://localhost:" + FAKE_PORT;//rabbitContainer.getConnectionString();
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
    void withPublishPlugin() throws Exception {
        //recordPlugin.setActive(false);
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


        var publish = (AmqpPublishPluginApis) publishPlugin.getApiHandler().stream().filter(
                a->a instanceof AmqpPublishPluginApis
        ).findFirst().get();
        var res = new Response();
        publish.getConnections(new Request(), res);
        var responses = mapper.deserialize(res.getResponseText(), new TypeReference<List<AmqpConnection>>() {
        });
        assertEquals(1, responses.size());
        var response = responses.get(0);
        var pm = new PublishAmqpMessage();
        pm.setAppId("test");
        pm.setContentType("text/plain");
        pm.setBody("TestData1");
        publish.doPublish(pm, response.getId(), response.getChannel());
        pm.setBody("TestData2");
        publish.doPublish(pm, response.getId(), response.getChannel());
        pm.setBody("TestData3");
        publish.doPublish(pm, response.getId(), response.getChannel());


        System.out.println("a");

        Sleeper.sleep(100);
        System.out.println("CLOSE CHANNEL ------------------------------------------------");
        channel.close();
        Sleeper.sleep(100);
        System.out.println("CLOSE CONNNECTION ------------------------------------------------");
        connection.close();


        Sleeper.sleep(1000, () -> messages.size() == 3);

        assertEquals(3, messages.size());
        assertTrue(messages.containsValue("TestData1"));
        assertTrue(messages.containsValue("TestData2"));
        assertTrue(messages.containsValue("TestData3"));
    }
}
