package org.kendar.mqtt;

import io.moquette.broker.Server;
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import org.junit.jupiter.api.TestInfo;
import org.kendar.server.TcpServer;
import org.kendar.utils.Sleeper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BasicTest {
    private static Server mqttBroker;
    protected static List<InterceptPublishMessage> moquetteMessages = new ArrayList<>();

    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            final String decodedPayload = msg.getPayload().toString(UTF_8);
            System.out.println("Received on topic: [" + msg.getTopicName() + "] content: [" + decodedPayload+"] qos:[" + msg.getQos()+"]");
            moquetteMessages.add(msg);
        }

//        @Override
//        public void onSessionLoopError(Throwable error) {
//            System.out.println("Session event loop reported error: " + error);
//        }
    }

    protected static final int FAKE_PORT = 1884;
    //protected static RabbitMqImage rabbitContainer;
    protected static TcpServer protocolServer;

    public static void beforeClassBase() throws IOException {
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


    public static void beforeEachBase(TestInfo testInfo)  {

        moquetteMessages.clear();
        var baseProtocol = new MqttProtocol(FAKE_PORT);
        var proxy = new MqttProxy("tcp://localhost:1883",
                null, null);
        if (testInfo != null) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                proxy.setStorage(new MqttFileStorage(Path.of("target", "tests", className, method, dsp)));
            } else {
                proxy.setStorage(new MqttFileStorage(Path.of("target", "tests", className, method)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        while (!protocolServer.isRunning()) {
            Sleeper.sleep(100);
        }
    }

    public static void afterEachBase() {

        protocolServer.stop();
    }

    public static void afterClassBase() throws Exception {
        mqttBroker.stopServer();
    }
}
