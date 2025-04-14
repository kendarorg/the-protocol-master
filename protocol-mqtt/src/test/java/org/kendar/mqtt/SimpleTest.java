package org.kendar.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleTest extends MqttBasicTest {

    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/exit/";

    @BeforeAll
    public static void beforeClass() throws IOException {


    }

    @AfterAll
    public static void afterClass() throws Exception {

    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        try {
            beforeClassBaseInternalIntercept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        Sleeper.sleep(500);
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
        afterEachBase();
    }

    @Test
    void qos2Test() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var publisher = new MqttClient("tcp://localhost:1884", publisherId);

        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);

        var message = new MqttMessage(MESSAGE_CONTENT.getBytes(UTF_8));
        //message.setQos(2);
        message.setQos(2);
        message.setRetained(true);
        publisher.publish(TOPIC_NAME, message);
        Sleeper.sleep(2000, () -> moquetteMessages.size() > 0);
        publisher.disconnect();
        assertEquals(1, moquetteMessages.size());
        var founded = moquetteMessages.get(0);
        assertEquals(MqttQoS.EXACTLY_ONCE, founded.getQos());
        assertEquals(MESSAGE_CONTENT, founded.getPayload().toString(UTF_8));
    }


    @Test
    void qos1Test() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var publisher = new MqttClient("tcp://localhost:1884", publisherId);

        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);

        var message = new MqttMessage(MESSAGE_CONTENT.getBytes(UTF_8));
        //message.setQos(2);
        message.setQos(1);
        message.setRetained(true);
        publisher.publish(TOPIC_NAME, message);
        Sleeper.sleep(1000, () -> moquetteMessages.size() > 0);
        publisher.disconnect();
        assertEquals(1, moquetteMessages.size());
        var founded = moquetteMessages.get(0);
        assertEquals(MqttQoS.AT_LEAST_ONCE, founded.getQos());
        assertEquals(MESSAGE_CONTENT, founded.getPayload().toString(UTF_8));
    }

    @Test
    void qos0Test() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var publisher = new MqttClient("tcp://localhost:1884", publisherId);

        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);

        var message = new MqttMessage(MESSAGE_CONTENT.getBytes(UTF_8));
        //message.setQos(2);
        message.setQos(0);
        message.setRetained(true);
        publisher.publish(TOPIC_NAME, message);
        Sleeper.sleep(1000, () -> moquetteMessages.size() > 0);
        publisher.disconnect();
        assertEquals(1, moquetteMessages.size());
        var founded = moquetteMessages.get(0);
        assertEquals(MqttQoS.AT_MOST_ONCE, founded.getQos());
        assertEquals(MESSAGE_CONTENT, founded.getPayload().toString(UTF_8));
    }


    @Test
    void pingTest() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var publisher = new MqttClientHack("tcp://localhost:1884", publisherId);

        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);

        publisher.pingreq();
        Sleeper.sleep(1000);
        publisher.disconnect();
    }
}
