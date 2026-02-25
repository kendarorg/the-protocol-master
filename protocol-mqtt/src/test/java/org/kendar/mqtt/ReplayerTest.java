package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kendar.mqtt.plugins.MqttReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.N;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ReplayerTest {
    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/subscribe/";
    private static final List<MqttMessage> messages = new ArrayList<>();

    private static void setupCallBack(MqttClient client) {
        messages.clear();
        client.setCallback(new MqttCallback() {
            public void messageArrived(String topic, MqttMessage message) {
                System.out.println("topic: " + topic);
                System.out.println("qos: " + message.getQos());
                System.out.println("message content: " + new String(message.getPayload()));
                messages.add(message);
            }

            public void connectionLost(Throwable cause) {
                System.out.println("connectionLost: " + cause.getMessage());
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("deliveryComplete: " + token.isComplete());
            }
        });
    }


    @Test
    void qos0Test() throws MqttException {
        messages.clear();
        var baseProtocol = new MqttProtocol(1883);
        var proxy = new MqttProxy();
        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "qos0Test"));
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var pl = new MqttReplayPlugin(new JsonMapper(), storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new NettyServer(baseProtocol);
        try {

            protocolServer.start();
            Sleeper.sleep(5000, protocolServer::isRunning);

            String publisherId = UUID.randomUUID().toString();
            var client = new MqttClient("tcp://localhost:1883", publisherId);

            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            Sleeper.sleep(1000, () -> client.isConnected());
            setupCallBack(client);

            client.subscribe(TOPIC_NAME, 0);
            Sleeper.sleep(1000);
            MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(0);
            client.publish(TOPIC_NAME, message);

            Sleeper.sleep(1000, () -> !messages.isEmpty());
            client.disconnect();
            client.close();
            assertEquals(1, messages.size());
            var mesg = messages.get(0);
            assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            assertEquals(0, mesg.getQos());
        } finally {
            protocolServer.stop();
        }

    }


    @Test
    void qos1Test() throws MqttException {
        messages.clear();
        var baseProtocol = new MqttProtocol(1884);
        var proxy = new MqttProxy();

        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "qos1Test"));
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var pl = new MqttReplayPlugin(new JsonMapper(), storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.of(pl));
        pl.setActive(true);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new NettyServer(baseProtocol);

        protocolServer.start();
        try {
            Sleeper.sleep(5000, protocolServer::isRunning);

            String publisherId = UUID.randomUUID().toString();
            var client = new MqttClient("tcp://localhost:1884", publisherId);

            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            if (client.isConnected()) {
                setupCallBack(client);

                client.subscribe(TOPIC_NAME, 1);

                MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
                message.setQos(1);
                client.publish(TOPIC_NAME, message);
            }
            Sleeper.sleep(1000, () -> !messages.isEmpty());
            client.disconnect();
            client.close();
            assertEquals(1, messages.size());
            var mesg = messages.get(0);
            assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            assertEquals(1, mesg.getQos());
        } finally {
            protocolServer.stop();
        }
    }


    @Test
    void qos2Test() throws MqttException {
        messages.clear();
        var baseProtocol = new MqttProtocol(1885);
        var proxy = new MqttProxy();
        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "qos2Test"));
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var pl = new MqttReplayPlugin(new JsonMapper(), storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicAysncReplayPluginSettings());
        proxy.setPluginHandlers(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new NettyServer(baseProtocol);

        try {
            protocolServer.start();

            Sleeper.sleep(5000, protocolServer::isRunning);

            String publisherId = UUID.randomUUID().toString();
            var client = new MqttClient("tcp://localhost:1885", publisherId);

            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);
            Sleeper.sleep(300, () -> client.isConnected());
            setupCallBack(client);

            client.subscribe(TOPIC_NAME, 2);

            MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(2);
            client.publish(TOPIC_NAME, message);

            Sleeper.sleep(6000, () -> !messages.isEmpty());
            client.disconnect();
            client.close();
            assertEquals(1, messages.size());
            var mesg = messages.get(0);
            assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            assertEquals(2, mesg.getQos());
        } finally {
            protocolServer.stop();
        }

    }
}
