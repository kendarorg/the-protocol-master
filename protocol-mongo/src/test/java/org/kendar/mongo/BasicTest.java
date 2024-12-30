package org.kendar.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mongo.plugins.MongoRecordPlugin;
import org.kendar.mongo.plugins.MongoReportPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tests.testcontainer.images.MongoDbImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 27077;
    protected static MongoDbImage mongoContainer;
    protected static TcpServer protocolServer;
    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        mongoContainer = new MongoDbImage();
        mongoContainer
                .withNetwork(network)
                .waitingForPort(27017)
                .start();

        Sleeper.sleep(5000, () -> {
            try {
                var settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(
                                mongoContainer.getConnectionString()))
                        //.serverApi(serverApi)
                        .build();

                MongoClient mongo = MongoClients.create(settings);
                try {
                    mongo.listDatabaseNames().iterator().hasNext();
                } catch (Exception e) {
                    return false;
                }
                mongo.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static void beforeEachBase(TestInfo testInfo) {
        var baseProtocol = new MongoProtocol(FAKE_PORT);
        var proxy = new MongoProxy(mongoContainer.getConnectionString());
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
        //gs.putService("storage", storage);
        var mapper = new JsonMapper();
        var pl = new MongoRecordPlugin(mapper,storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());
        var rep = new MongoReportPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        rep.setActive(true);
        proxy.setPlugins(List.of(pl, rep));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        baseProtocol.initialize();
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

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }
}
