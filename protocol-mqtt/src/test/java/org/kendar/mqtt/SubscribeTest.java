package org.kendar.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscribeTest extends BasicTest {

    public static final String MESSAGE_CONTENT = "Hello World!!";
    public static final String TOPIC_NAME = "/subscribe/";
    private static final List<Mqtt3Publish> messages = new ArrayList<Mqtt3Publish>();

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

    private static void setupCallBack(Mqtt3BlockingClient client, String topicName, MqttQos qos) throws ExecutionException, InterruptedException {
        messages.clear();

        Mqtt3AsyncClient asyncClient = client.toAsync();

        final Mqtt3Subscribe subscription =
                Mqtt3Subscribe.builder()
                        .addSubscription()
                        .topicFilter("test/1")
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .applySubscription()
                        .build();
        Consumer<Mqtt3Publish> subCallback = (Mqtt3Publish msg) -> {
            //System.out.println("Sub COMMON callback: " + publish.getTopic() + " " + publish.getPayload());
            final String decodedPayload = msg.getPayloadAsBytes().toString(UTF_8);
            System.out.println("Received on topic: [" + msg.getTopic() + "] content: [" +
                    decodedPayload + "] qos:[" + msg.getQos() + "]");
            messages.add(msg);
        };
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
    void qos0Test() throws ExecutionException, InterruptedException {
        String publisherId = UUID.randomUUID().toString();
        var client =  Mqtt3Client.builder()
                .identifier(publisherId)
                .serverHost("localhost")
                .serverPort(1884)
                .buildBlocking();


        setupCallBack(client,TOPIC_NAME,MqttQos.AT_MOST_ONCE);

        client.connect();

        Sleeper.sleep(500);
        System.out.println("========================");

        var publishMessage = Mqtt3Publish.builder()
                .topic(TOPIC_NAME)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(MESSAGE_CONTENT.getBytes())
                .build();
        client.publish(publishMessage);

        Sleeper.sleep(500);

        System.out.println("========================");

        Sleeper.sleep(7000, () -> messages.size() > 0);
        client.disconnect();
        assertEquals(1, messages.size());
        Mqtt3Publish mesg = messages.get(0);
        assertEquals(MESSAGE_CONTENT, new String(mesg.getPayloadAsBytes()));
        assertEquals(1, mesg.getQos());

    }
}
