package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SimpleTest extends BasicTest{
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
    void simpleTest() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        var publisher = new MqttClient("tcp://localhost:1884",publisherId);

        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);

        var message = new MqttMessage("Hello World!!".getBytes(UTF_8));
        message.setQos(2);
        message.setRetained(true);
        publisher.publish("/exit",message);
    }
}
