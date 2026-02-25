package org.kendar.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.*;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mqtt.plugins.MqttReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("RedundantThrows")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ReplayerNotBlockingTest extends MqttBasicTest {

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

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {

        try {
            beforeClassBase();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        beforeEachNotStarting(testInfo);
    }

    @AfterEach
    public void afterEach() {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
        EventsQueue.unregister("recorder", ReportDataEvent.class);
        events.clear();
    }


    @Test
    void qos2Test() throws MqttException {
        messages.clear();
        var baseProtocol = new MqttProtocol(1885);
        var proxy = new MqttProxy("tcp://localhost:1883",
                null, null);
        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "qos2Test"));
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var settings = new BasicAysncReplayPluginSettings();
        settings.setBlockExternal(false);
        var pl = new MqttReplayPlugin(new JsonMapper(), storage).initialize(gs, new ByteProtocolSettingsWithLogin(), settings);
        proxy.setPluginHandlers(List.of(pl));

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new NettyServer(baseProtocol);
        pl.setActive(true);

        try {
            protocolServer.start();

            Sleeper.sleep(5000, protocolServer::isRunning);

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
            Sleeper.sleep(1000, () -> !messages.isEmpty());
            client.disconnect();
            client.close();
            Assertions.assertEquals(1, messages.size());
            var mesg = messages.get(0);
            Assertions.assertEquals(MESSAGE_CONTENT, new String(mesg.getPayload()));
            Assertions.assertEquals(2, mesg.getQos());
        } finally {
            protocolServer.stop();
        }

    }
}
