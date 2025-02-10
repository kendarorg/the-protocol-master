package org.kendar.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.*;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.mqtt.apis.MqttPublishPluginApis;
import org.kendar.mqtt.apis.dtos.MqttConnection;
import org.kendar.mqtt.apis.dtos.PublishMqttMessage;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SubscribeTest extends BasicTest {

    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/subscribe/";
    private static final List<MqttMessage> messages = new ArrayList<>();
    private static JsonMapper mapper = new JsonMapper();

    @BeforeAll
    public static void beforeClass() throws IOException {


    }

    @AfterAll
    public static void afterClass() throws Exception {

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
        try {
            beforeClassBase();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {

        try {
            afterClassBase();
        } catch (Exception ex) {

        }
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


        var events = getEvents().stream().collect(Collectors.toList());
        Assertions.assertEquals(4, events.size());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("SEND")).count());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("CONNECT")).count());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("SUBSCRIBE")).count());

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

    @Test
    void qos2TestPublish() throws MqttException {
        publishPlugin.setActive(true);
        String publisherId = UUID.randomUUID().toString();
        var client = new MqttClient("tcp://localhost:1884", publisherId);

        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);

        if (client.isConnected()) {
            setupCallBack(client);

            client.subscribe(TOPIC_NAME, 2);
            Sleeper.sleep(500);
            System.out.println("========================");

            /*MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(2);
            client.publish(TOPIC_NAME, message);*/
            var publish = (MqttPublishPluginApis) publishPlugin.getApiHandler().stream().filter(
                    a -> a instanceof MqttPublishPluginApis
            ).findFirst().get();
            var res = new Response();
            publish.getConnections(new Request(), res);
            var responses = mapper.deserialize(res.getResponseText(), new TypeReference<List<MqttConnection>>() {
            });
            Assertions.assertEquals(1, responses.size());
            var response = responses.get(0);
            var pm = new PublishMqttMessage();
            pm.setContentType("text/plain");
            pm.setBody(MESSAGE_CONTENT);
            publish.doPublish(pm, response.getId(), TOPIC_NAME);
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


        var events = getEvents().stream().collect(Collectors.toList());
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("CONNECT")).count());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("SUBSCRIBE")).count());
        publishPlugin.setActive(false);
    }

    @Test
    void qos1TestPublish() throws MqttException {
        publishPlugin.setActive(true);
        String publisherId = UUID.randomUUID().toString();
        var client = new MqttClient("tcp://localhost:1884", publisherId);

        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);

        if (client.isConnected()) {
            setupCallBack(client);

            client.subscribe(TOPIC_NAME, 1);
            Sleeper.sleep(500);
            System.out.println("========================");

            /*MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(2);
            client.publish(TOPIC_NAME, message);*/
            var publish = (MqttPublishPluginApis) publishPlugin.getApiHandler().stream().filter(
                    a -> a instanceof MqttPublishPluginApis
            ).findFirst().get();
            var res = new Response();
            publish.getConnections(new Request(), res);
            var responses = mapper.deserialize(res.getResponseText(), new TypeReference<List<MqttConnection>>() {
            });
            Assertions.assertEquals(1, responses.size());
            var response = responses.get(0);
            var pm = new PublishMqttMessage();
            pm.setContentType("text/plain");
            pm.setBody(MESSAGE_CONTENT);
            publish.doPublish(pm, response.getId(), TOPIC_NAME);
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
        assertEquals(1, mesg.getQos());


        var events = getEvents().stream().collect(Collectors.toList());
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("CONNECT")).count());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("SUBSCRIBE")).count());
        publishPlugin.setActive(false);
    }


    @Test
    void qos0TestPublish() throws MqttException {
        publishPlugin.setActive(true);
        String publisherId = UUID.randomUUID().toString();
        var client = new MqttClient("tcp://localhost:1884", publisherId);

        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);

        if (client.isConnected()) {
            setupCallBack(client);

            client.subscribe(TOPIC_NAME, 0);
            Sleeper.sleep(500);
            System.out.println("========================");

            /*MqttMessage message = new MqttMessage(MESSAGE_CONTENT.getBytes());
            message.setQos(2);
            client.publish(TOPIC_NAME, message);*/
            var publish = (MqttPublishPluginApis) publishPlugin.getApiHandler().stream().filter(
                    a -> a instanceof MqttPublishPluginApis
            ).findFirst().get();
            var res = new Response();
            publish.getConnections(new Request(), res);
            var responses = mapper.deserialize(res.getResponseText(), new TypeReference<List<MqttConnection>>() {
            });
            Assertions.assertEquals(1, responses.size());
            var response = responses.get(0);
            var pm = new PublishMqttMessage();
            pm.setContentType("text/plain");
            pm.setBody(MESSAGE_CONTENT);
            publish.doPublish(pm, response.getId(), TOPIC_NAME);
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
        assertEquals(0, mesg.getQos());


        var events = getEvents().stream().collect(Collectors.toList());
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("CONNECT")).count());
        Assertions.assertEquals(1, events.stream().filter(e -> e.getQuery().startsWith("SUBSCRIBE")).count());
        publishPlugin.setActive(false);
    }
}
