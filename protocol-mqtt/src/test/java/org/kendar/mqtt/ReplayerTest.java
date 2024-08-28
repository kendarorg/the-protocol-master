package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kendar.server.TcpServer;
import org.kendar.utils.Sleeper;

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
            public void messageArrived(String topic, MqttMessage message) throws Exception {
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
        proxy.setStorage(new MqttFileStorage(Path.of("src",
                "test", "resources", "qos0Test")));

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);
        try {

            protocolServer.start();
            while (!protocolServer.isRunning()) {
                Sleeper.sleep(100);
            }

            String publisherId = UUID.randomUUID().toString();
            var client = new MqttClient("tcp://localhost:1883", publisherId);

            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            if (client.isConnected()) {
                setupCallBack(client);

                client.subscribe(TOPIC_NAME, 0);

                MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
                message.setQos(0);
                client.publish(TOPIC_NAME, message);
            }
            Sleeper.sleep(1000, () -> messages.size() > 0);
            client.disconnect();
            client.close();
            assertEquals(1, messages.size());
            var mesg = messages.get(0);
            assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            assertEquals(0, mesg.getQos());
        }finally {
            protocolServer.stop();
        }

    }


    @Test
    void qos1Test() throws MqttException {
        messages.clear();
        var baseProtocol = new MqttProtocol(1884);
        var proxy = new MqttProxy();
        proxy.setStorage(new MqttFileStorage(Path.of("src",
                "test", "resources", "qos1Test")));

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        try {
            while (!protocolServer.isRunning()) {
                Sleeper.sleep(100);
            }

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
            Sleeper.sleep(1000, () -> messages.size() > 0);
            client.disconnect();
            client.close();
            assertEquals(1, messages.size());
            var mesg = messages.get(0);
            assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            assertEquals(1, mesg.getQos());
        }finally {
            protocolServer.stop();
        }
    }


    @Test
    void qos2Test() throws MqttException {
        messages.clear();
        var baseProtocol = new MqttProtocol(1885);
        var proxy = new MqttProxy();
        proxy.setStorage(new MqttFileStorage(Path.of("src",
                "test", "resources", "qos2Test")));

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        try {
            protocolServer.start();

            while (!protocolServer.isRunning()) {
                Sleeper.sleep(100);
            }

            String publisherId = UUID.randomUUID().toString();
            var client = new MqttClient("tcp://localhost:1885", publisherId);

            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            if (client.isConnected()) {
                setupCallBack(client);

                client.subscribe(TOPIC_NAME, 2);

                MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
                message.setQos(2);
                client.publish(TOPIC_NAME, message);
            }
            Sleeper.sleep(1000, () -> messages.size() > 0);
            client.disconnect();
            client.close();
            assertEquals(1, messages.size());
            var mesg = messages.get(0);
            assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            assertEquals(2, mesg.getQos());
        }finally {
            protocolServer.stop();
        }

    }
}
