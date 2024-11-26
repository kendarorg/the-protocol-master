package org.kendar.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.kendar.mongo.plugins.MongoReplayPlugin;
import org.kendar.server.TcpServer;
import org.kendar.storage.FileStorageRepository;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReplayerTest {

    protected static final int FAKE_PORT = 27079;

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

    @Test
    void testConnectionWithProtocolWithServerApis() {
        var baseProtocol = new MongoProtocol(FAKE_PORT);
        var proxy = new MongoProxy();

        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "replay"));
        storage.initialize();
        var pl = new MongoReplayPlugin().withStorage(storage);
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);


        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, protocolServer::isRunning);

        var c = getProxyConnectionWithServerApis();
        var db = c.getDatabase("admin");
        Bson command = new BsonDocument("dbStats", new BsonInt64(1));
        Document commandResult = db.runCommand(command);
        assertNotNull(commandResult);
        System.out.println(commandResult.toJson());
        var ol = db.getCollection("testcollection");
        var result = ol.insertOne(new Document()
                .append("_id", new ObjectId())
                .append("title", "Ski Bloopers")
                .append("genres", Arrays.asList("Documentary", "Comedy")));
        Bson filter = Filters.eq("_id", result.getInsertedId());
        Document doc = ol.find(filter).first();
        System.out.println(doc.toJson());
        assertEquals("Ski Bloopers", doc.get("title").toString());
        assertNotNull(doc);
        assertNotNull(c);
        protocolServer.stop();
    }
}
