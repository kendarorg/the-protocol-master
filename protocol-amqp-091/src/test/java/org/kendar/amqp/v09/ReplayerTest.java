package org.kendar.amqp.v09;

import com.rabbitmq.client.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kendar.amqp.v09.plugins.AmqpReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ReplayerTest {


    public static final String MAIN_QUEUE = "fuffa_queue";
    public static final String DEFAULT_MESSAGE_CONTENT = "zzzz details";

    protected static final int FAKE_PORT = 5682;

    private static Channel consume(ConnectionFactory connectionFactory, ConcurrentHashMap<Integer, String> messages) throws IOException, TimeoutException {
        var connectionConsume = connectionFactory.newConnection();
        var channelConsume = connectionConsume
                .openChannel()
                .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
        channelConsume.queueDeclare(MAIN_QUEUE, false, false, false, null);

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
                channelConsume.basicAck(envelope.getDeliveryTag(), false);
                // process the message
            }
        };
        channelConsume.basicConsume(MAIN_QUEUE, false, consumer);
        return channelConsume;
    }

    @Test
    void test2_differentChannelAndConnection() throws Exception {
        var messages = new ConcurrentHashMap<Integer, String>();
        String exectedMessage = DEFAULT_MESSAGE_CONTENT;
        var baseProtocol = new AmqpProtocol(FAKE_PORT);
        var proxy = new AmqpProxy();

        StorageRepository storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "test2_differentChannelAndConnection"));
        storage.initialize();
        var gs = new GlobalSettings();
        gs.putService("storage", storage);
        var pl = new AmqpReplayPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, protocolServer::isRunning);
        try {

            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.enableHostnameVerification();
            var cs = "amqp://localhost:" + FAKE_PORT;//rabbitContainer.getConnectionString();
            connectionFactory.setUri(cs);
            connectionFactory.setPassword("test");

            Sleeper.sleep(100);

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


            Sleeper.sleep(1000, () -> messages.size() == 3);

            assertEquals(3, messages.size());
            assertTrue(messages.containsValue(exectedMessage + "1"));
            assertTrue(messages.containsValue(exectedMessage + "2"));
            assertTrue(messages.containsValue(exectedMessage + "3"));
        } finally {

            protocolServer.stop();
        }
    }

    @Test
    void test5_noPublish() throws Exception {
        var messages = new ConcurrentHashMap<Integer, String>();
        String exectedMessage = DEFAULT_MESSAGE_CONTENT;
        var baseProtocol = new AmqpProtocol(FAKE_PORT);
        var proxy = new AmqpProxy();
        StorageRepository storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "test5_noPublish"));
        storage.initialize();
        var gs = new GlobalSettings();
        gs.putService("storage", storage);
        var pl = new AmqpReplayPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        try {
            Sleeper.sleep(5000, protocolServer::isRunning);
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.enableHostnameVerification();
            var cs = "amqp://localhost:" + FAKE_PORT;//rabbitContainer.getConnectionString();
            connectionFactory.setUri(cs);
            connectionFactory.setPassword("test");

            Sleeper.sleep(100);

            var chanConsume = consume(connectionFactory, messages);


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
            //SimpleProxyServer.write=true;
            Sleeper.sleep(100);
            System.out.println("------------------------------------------------------------");
            Sleeper.sleep(100);
            channel.close();
            connection.close();


            Sleeper.sleep(1000, () -> messages.size() == 3);

            assertEquals(3, messages.size());
            assertTrue(messages.containsValue(exectedMessage + "1"));
            assertTrue(messages.containsValue(exectedMessage + "2"));
            assertTrue(messages.containsValue(exectedMessage + "3"));

        } finally {

            protocolServer.stop();
        }
    }

    @Test
    void test3_openConnection() throws Exception {

        var baseProtocol = new AmqpProtocol(FAKE_PORT);
        var proxy = new AmqpProxy();
        StorageRepository storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "test3_openConnection"));
        storage.initialize();
        var gs = new GlobalSettings();
        gs.putService("storage", storage);
        var pl = new AmqpReplayPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        try {
            Sleeper.sleep(5000, protocolServer::isRunning);

            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.enableHostnameVerification();
            var cs = "amqp://localhost:" + FAKE_PORT;//rabbitContainer.getConnectionString();
            connectionFactory.setUri(cs);
            connectionFactory.setPassword("test");
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection
                    .openChannel()
                    .orElseThrow(() -> new RuntimeException("Failed to Open channel"));
            channel.close();
            connection.close();
        } finally {

            protocolServer.stop();
        }
    }
}
