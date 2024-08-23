package org.kendar.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class SimpleTest extends BasicTest {

    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/exit/";

    @BeforeAll
    public static void beforeClass() throws IOException {
        beforeClassBaseInternalIntercept();

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
    void qos0Test() {
        String publisherId = UUID.randomUUID().toString();
        var client =  Mqtt3Client.builder()
                .identifier(publisherId)
                .serverHost("localhost")
                .serverPort(1884)
                .buildBlocking();

        client.connect();

        var publishMessage = Mqtt3Publish.builder()
                .topic(TOPIC_NAME)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(MESSAGE_CONTENT.getBytes())
                .build();
        client.publish(publishMessage);
        Sleeper.sleep(7000, () -> moquetteMessages.size() > 0);
        client.disconnect();
        assertEquals(1, moquetteMessages.size());
        var founded = moquetteMessages.get(0);
        assertEquals(MqttQoS.AT_MOST_ONCE, founded.getQos());
        assertEquals(MESSAGE_CONTENT, founded.getPayload().toString(UTF_8));
    }


    @Test
    void qos1Test() {
        String publisherId = UUID.randomUUID().toString();
        var client =  Mqtt3Client.builder()
                .identifier(publisherId)
                .serverHost("localhost")
                .serverPort(1884)
                .buildBlocking();

        client.connect();

        var publishMessage = Mqtt3Publish.builder()
                .topic(TOPIC_NAME)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(MESSAGE_CONTENT.getBytes())
                .build();
        client.publish(publishMessage);
        Sleeper.sleep(7000, () -> moquetteMessages.size() > 0);
        client.disconnect();
        assertEquals(1, moquetteMessages.size());
        var founded = moquetteMessages.get(0);
        assertEquals(MqttQoS.AT_LEAST_ONCE, founded.getQos());
        assertEquals(MESSAGE_CONTENT, founded.getPayload().toString(UTF_8));
    }


    @Test
    void qos2Test() {
        String publisherId = UUID.randomUUID().toString();
        var client =  Mqtt3Client.builder()
                .identifier(publisherId)
                .serverHost("localhost")
                .serverPort(1884)
                .buildBlocking();

        client.connect();

        var publishMessage = Mqtt3Publish.builder()
                .topic(TOPIC_NAME)
                .qos(MqttQos.EXACTLY_ONCE)
                .payload(MESSAGE_CONTENT.getBytes())
                .build();
        client.publish(publishMessage);
        Sleeper.sleep(7000, () -> moquetteMessages.size() > 0);
        client.disconnect();
        assertEquals(1, moquetteMessages.size());
        var founded = moquetteMessages.get(0);
        assertEquals(MqttQoS.EXACTLY_ONCE, founded.getQos());
        assertEquals(MESSAGE_CONTENT, founded.getPayload().toString(UTF_8));
    }
}
