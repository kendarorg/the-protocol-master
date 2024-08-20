package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.UUID;

public class SubscribeTest extends BasicTest{

    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/subscribe/";
    public static final int SUB_QOS = 1;
    public static final int PUB_QOS = 1;

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

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void qos1Test() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var client = new MqttClient("tcp://localhost:1884",publisherId);

        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);

        if (client.isConnected()) {
            client.setCallback(new MqttCallback() {
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("topic: " + topic);
                    System.out.println("qos: " + message.getQos());
                    System.out.println("message content: " + new String(message.getPayload()));
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost: " + cause.getMessage());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete: " + token.isComplete());
                }
            });

            client.subscribe(TOPIC_NAME, SUB_QOS);

            MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(PUB_QOS);
            client.publish(TOPIC_NAME, message);
        }

        client.disconnect();
        client.close();
    }
}
