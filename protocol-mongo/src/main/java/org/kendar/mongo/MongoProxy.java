package org.kendar.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.kendar.iterators.ProcessId;
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.mongo.dtos.OpMsgSection;
import org.kendar.mongo.dtos.OpQueryContent;
import org.kendar.mongo.dtos.OpReplyContent;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.utils.MongoStorage;
import org.kendar.mongo.utils.NullMongoStorage;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.concurrent.atomic.AtomicInteger;

public class MongoProxy extends Proxy<MongoStorage> {
    private static final JsonMapper mapper = new JsonMapper();
    private static final AtomicInteger connectionId = new AtomicInteger(1);
    private String connectionString;
    private ServerApiVersion serverApiVersion;
    private boolean replayer = false;

    public MongoProxy(String connectionString, ServerApiVersion serverApiVersion) {

        this.connectionString = connectionString;
        this.serverApiVersion = serverApiVersion;
        this.setStorage(new NullMongoStorage());
    }

    public MongoProxy(MongoStorage storage) {
        replayer = true;
        this.setStorage(storage);
    }

    public MongoProxy(String connectionString) {

        this(connectionString, ServerApiVersion.V1);
    }

    @Override
    public ProxyConnection connect(NetworkProtoContext context) {
        if (replayer) {
            return new ProxyConnection(null);
        }
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        return new ProxyConnection(MongoClients.create(settings));
    }

    @Override
    public void initialize() {

    }

    public OpMsgContent runGenericOpMsg(MongoProtoContext protoContext, OpMsgContent data,
                                        Class<?> prevState, MongoClient mongoClient, String db) {
        long start = System.currentTimeMillis();

        if (replayer) {
            var item = storage.read((JsonNode) data.serialize(), "OP_MSG");
            var res = new OpMsgContent();
            res.doDeserialize((JsonNode) item.getOutput(), mapper);
            res.setRequestId(protoContext.getReqResId());
            res.setResponseId(data.getRequestId());
            res.setFlags(8);
            return res;
        }

        var docPayload = data.getSections().get(0).getDocuments().get(0);
        var command = (BsonDocument) BsonDocument.parse(docPayload);
        var finalMessage = command.containsKey("endSession");
        if (command.get("isMaster") != null) {
            return null;
        }
        if (!data.getSections().isEmpty()) {

            for (var i = 1; i < data.getSections().size(); i++) {
                var pack = data.getSections().get(i);

                var tl = new BsonArray();
                for (var j = 0; j < pack.getDocuments().size(); j++) {
                    var bdoc = (BsonDocument) BsonDocument.parse(pack.getDocuments().get(j));
                    tl.add(bdoc);
                }
                command.put(pack.getIdentifier(), tl);
            }
        }

        command.remove("$db");
        command.remove("lsid");
        command.remove("$clusterTime");
        command.remove("apiVersion");


        var database = mongoClient.getDatabase(db);
        Document commandResult = database.runCommand(command);

        var toSend = new OpMsgContent(0, protoContext.getReqResId(), data.getRequestId());
        OpMsgSection section = new OpMsgSection();
        section.getDocuments().add(commandResult.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()));
        toSend.getSections().add(section);
        long end = System.currentTimeMillis();

        this.storage.write((JsonNode) data.serialize(), (JsonNode) toSend.serialize(), end - start, "OP_MSG", "MONGODB");
        return toSend;
    }

    public OpMsgContent runHelloOpMsg(MongoProtoContext protoContext, OpMsgContent lsatOp, Class<?> prevState, MongoClient mongoClient, String dbName) {
        long start = System.currentTimeMillis();

        if (replayer) {
            var item = storage.read((JsonNode) lsatOp.serialize(), "HELLO_OP_MSG");
            var res = new OpMsgContent();
            res.doDeserialize((JsonNode) item.getOutput(), mapper);
            res.setRequestId(protoContext.getReqResId());
            res.setResponseId(lsatOp.getRequestId());
            res.setFlags(8);
            return res;
        }
        var dbInstance = mongoClient.getDatabase(dbName);
        var serverDescription = mongoClient.getClusterDescription().getServerDescriptions().get(0);
        Bson findCommand = new BsonDocument("hostInfo", new BsonInt64(1));

        var commandResult = dbInstance.runCommand(findCommand);

        var pid = (ProcessId) protoContext.getValue("MONGO_PID");

        var resultMap = new Document();
        resultMap.put("ok", true);
        resultMap.put("ismaster", true);
        resultMap.put("maxBsonObjectSize", serverDescription.getMaxDocumentSize());
        resultMap.put("maxMessageSizeBytes", 48000000);
        resultMap.put("maxWriteBatchSize", 100000);
        resultMap.put("localTime", ((Document) commandResult.get("system")).get("currentTime"));
        resultMap.put("logicalSessionTimeoutMinutes", mongoClient.getClusterDescription().getLogicalSessionTimeoutMinutes());
        resultMap.put("connectionId", pid.getPid());
        resultMap.put("minWireVersion", 0);
        resultMap.put("maxWireVersion", 8);
        resultMap.put("readOnly", false);
        // resultMap.put("saslSupportedMechs", List.of("PLAIN"));
        var json = resultMap.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build());
        var toSend = new OpMsgContent(0, protoContext.getReqResId(), lsatOp.getRequestId());
        OpMsgSection section = new OpMsgSection();
        section.getDocuments().add(json);
        toSend.getSections().add(section);
        long end = System.currentTimeMillis();
        this.storage.write((JsonNode) lsatOp.serialize(), (JsonNode) toSend.serialize(), end - start, "HELLO_OP_MSG", "MONGODB");
        return toSend;
    }

    public OpReplyContent runHelloOpQuery(MongoProtoContext protoContext, OpQueryContent lsatOp, Class<?> prevState, MongoClient mongoClient, String dbName) {
        long start = System.currentTimeMillis();
        if (replayer) {
            var item = storage.read((JsonNode) lsatOp.serialize(), "HELLO_OP_QUERY");
            var res = new OpReplyContent();
            res.doDeserialize((JsonNode) item.getOutput(), mapper);
            res.setRequestId(protoContext.getReqResId());
            res.setResponseId(lsatOp.getRequestId());
            res.setFlags(8);
            return res;
        }
        var dbInstance = mongoClient.getDatabase(dbName);
        var serverDescription = mongoClient.getClusterDescription().getServerDescriptions().get(0);
        Bson findCommand = new BsonDocument("hostInfo", new BsonInt64(1));

        var commandResult = dbInstance.runCommand(findCommand);

        var pid = (ProcessId) protoContext.getValue("MONGO_PID");

        var resultMap = new Document();
        resultMap.put("ismaster", true);
        resultMap.put("maxBsonObjectSize", serverDescription.getMaxDocumentSize());
        resultMap.put("maxMessageSizeBytes", 48000000);
        resultMap.put("maxWriteBatchSize", 100000);
        resultMap.put("localTime", ((Document) commandResult.get("system")).get("currentTime"));
        resultMap.put("logicalSessionTimeoutMinutes", mongoClient.getClusterDescription().getLogicalSessionTimeoutMinutes());
        resultMap.put("connectionId", pid.getPid());
        resultMap.put("minWireVersion", 0);
        resultMap.put("maxWireVersion", 8);
        resultMap.put("readOnly", false);
        resultMap.put("ok", 1.0);
        //resultMap.put("saslSupportedMechs", List.of("PLAIN"));
        var json = resultMap.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build());
        var toSend = new OpReplyContent(8, protoContext.getReqResId(), lsatOp.getRequestId());
        toSend.setCursorId(0);
        toSend.getDocuments().add(json);

        long end = System.currentTimeMillis();
        this.storage.write((JsonNode) lsatOp.serialize(), (JsonNode) toSend.serialize(), end - start, "HELLO_OP_QUERY", "MONGODB");
        return toSend;
    }
}
