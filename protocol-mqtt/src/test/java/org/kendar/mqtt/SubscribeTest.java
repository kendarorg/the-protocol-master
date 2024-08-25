package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class SubscribeTest extends BasicTest {

    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/subscribe/";
    private static final List<MqttMessage> messages = new ArrayList<>();

    @BeforeAll
    public static void beforeClass() throws IOException {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
    }

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

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }


    @Test
    void qos2Test() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var client = new MqttClient("tcp://localhost:1884", publisherId);

        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);

        if (client.isConnected()) {
            setupCallBack(client);

            client.subscribe(TOPIC_NAME, 2);
            Sleeper.sleep(500);
            System.out.println("========================");

            MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(2);
            client.publish(TOPIC_NAME, message);
            Sleeper.sleep(500);

            System.out.println("========================");
        } else {
            throw new RuntimeException("NOT CONNETED");
        }
        Sleeper.sleep(7000, () -> messages.size() > 0);
        client.disconnect();
        client.close();
        assertEquals(1, messages.size());
        var mesg = messages.get(0);
        assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
        assertEquals(2, mesg.getQos());

    }

    @Test
    void qos1Test() throws MqttException {
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

    }

    @Test
    void qos0Test() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var client = new MqttClient("tcp://localhost:1884", publisherId);

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
    }
}
