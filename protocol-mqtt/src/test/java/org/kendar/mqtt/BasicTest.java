package org.kendar.mqtt;

import io.moquette.broker.Server;
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mqtt.plugins.MqttRecordPlugin;
import org.kendar.mqtt.plugins.MqttReportPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BasicTest {
    protected static final int FAKE_PORT = 1884;
    protected static List<InterceptPublishMessage> moquetteMessages = new ArrayList<>();
    //protected static RabbitMqImage rabbitContainer;
    protected static TcpServer protocolServer;
    private static Server mqttBroker;
    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();

    public static void beforeClassBaseInternalIntercept() throws IOException {
        //LoggerBuilder.setLevel(Logger.ROOT_LOGGER_NAME, Level.DEBUG);
        //var classpathLoader = new ClasspathResourceLoader();
        //final var classPathConfig = new ResourceLoaderConfig(classpathLoader);
        var classpathLoader = new FileResourceLoader(new File("moquette.conf"));
        final var classPathConfig = new ResourceLoaderConfig(classpathLoader);

        mqttBroker = new Server();
        List<? extends InterceptHandler> userHandlers = Collections.singletonList(new PublisherListener());
        mqttBroker.startServer(classPathConfig, userHandlers);

        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping broker");
            mqttBroker.stopServer();
            System.out.println("Broker stopped");
        }));
//
//        var dockerHost = Utils.getDockerHost();
//        assertNotNull(dockerHost);
//        var network = Network.newNetwork();
//        rabbitContainer = new RabbitMqImage();
//        rabbitContainer
//                .withNetwork(network)
//                .waitingForPort(5672)
//                .start();


    }

    public static void beforeClassBase() throws IOException {
        var classpathLoader = new FileResourceLoader(new File("moquette.conf"));
        final var classPathConfig = new ResourceLoaderConfig(classpathLoader);

        mqttBroker = new Server();
        List<? extends InterceptHandler> userHandlers = new ArrayList<>();
        mqttBroker.startServer(classPathConfig, userHandlers);

        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping broker");
            mqttBroker.stopServer();
            System.out.println("Broker stopped");
        }));
    }

    public static void beforeEachBase(TestInfo testInfo) {
        events.clear();
        moquetteMessages.clear();
        var baseProtocol = new MqttProtocol(FAKE_PORT);
        var proxy = new MqttProxy("tcp://localhost:1883",
                null, null);
        StorageRepository storage = new NullStorageRepository();
        if (testInfo != null && testInfo.getTestClass().isPresent() &&
                testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                storage = new FileStorageRepository(Path.of("target", "tests", className, method, dsp));
            } else {
                storage = new FileStorageRepository(Path.of("target", "tests", className, method));
            }
        }
        storage.initialize();
        var gs = new GlobalSettings();
        gs.putService("storage", storage);
        var pl = new MqttRecordPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());
        var rep = new MqttReportPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        proxy.setPlugins(List.of(pl, rep));
        rep.setActive(true);
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {
        EventsQueue.unregister("recorder", ReportDataEvent.class);
        events.clear();
        protocolServer.stop();
    }

    public static void afterClassBase() throws Exception {
        mqttBroker.stopServer();
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }

    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            final String decodedPayload = msg.getPayload().toString(UTF_8);
            System.out.println("Received on topic: [" + msg.getTopicName() + "] content: [" + decodedPayload + "] qos:[" + msg.getQos() + "]");
            moquetteMessages.add(msg);
        }

//        @Override
//        public void onSessionLoopError(Throwable error) {
//            System.out.println("Session event loop reported error: " + error);
//        }
    }

}
