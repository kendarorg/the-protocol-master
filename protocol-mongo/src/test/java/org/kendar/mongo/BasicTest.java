package org.kendar.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.TestInfo;
import org.kendar.server.TcpServer;
import org.kendar.testcontainer.images.MongoDbImage;
import org.kendar.testcontainer.utils.Utils;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 27077;
    protected static MongoDbImage mongoContainer;
    protected static TcpServer protocolServer;

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        mongoContainer = new MongoDbImage();
        mongoContainer
                .withNetwork(network)
                .waitingForPort(27017)
                .start();


    }


    public static void beforeEachBase(TestInfo testInfo) {
        var baseProtocol = new MongoProtocol(FAKE_PORT);
        var proxy = new MongoProxy(mongoContainer.getConnectionString());
        if (testInfo != null) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                proxy.setStorage(new MongoFileStorage(Path.of("target", "tests", className, method, dsp)));
            } else {
                proxy.setStorage(new MongoFileStorage(Path.of("target", "tests", className, method)));
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
        mongoContainer.close();
    }

    protected static MongoClient getProxyConnection() {
//        var serverApi = ServerApi.builder()
//                .version(ServerApiVersion.V1)
//                .build();
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        "mongodb://localhost:" + FAKE_PORT + "/?retryWrites=false&retryReads=false&tls=false&ssl=false"))
                //.serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }

    protected static MongoClient getProxyConnectionAuth() {
//        var serverApi = ServerApi.builder()
//                .version(ServerApiVersion.V1)
//                .build();
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        "mongodb://frappa:password@localhost:" + FAKE_PORT + "/?" +
                                "retryWrites=false&retryReads=false&tls=false" +
                                "&ssl=false&authMechanism=PLAIN"))
                //.serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }

    protected static MongoClient getProxyConnectionWithServerApis() {
        var serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        "mongodb://localhost:" + FAKE_PORT + "/?retryWrites=false&retryReads=false&tls=false&ssl=false"))
                .serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }

    protected static MongoClient getRealConnection() {
//        var serverApi = ServerApi.builder()
//                .version(ServerApiVersion.V1)
//                .build();
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoContainer.getConnectionString()))
                //.serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }
}
