package org.kendar.mongo;

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
import org.kendar.filters.ProtocolPhase;
import org.kendar.iterators.ProcessId;
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.mongo.dtos.OpMsgSection;
import org.kendar.mongo.dtos.OpQueryContent;
import org.kendar.mongo.dtos.OpReplyContent;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.proxy.DocumentContainer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.FilterContext;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

public class MongoProxy extends Proxy {
    private static final JsonMapper mapper = new JsonMapper();
    private String connectionString;
    private ServerApiVersion serverApiVersion;

    public MongoProxy(String connectionString, ServerApiVersion serverApiVersion) {

        this.connectionString = connectionString;
        this.serverApiVersion = serverApiVersion;
    }

    public MongoProxy(String connectionString) {

        this(connectionString, ServerApiVersion.V1);
    }

    public MongoProxy() {

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
        var out = new OpMsgContent(0, protoContext.getReqResId(), data.getRequestId());

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

        var cmdContainer = new DocumentContainer(protoContext.getReqResId(), data.getRequestId());

        var filterContext = new FilterContext("MONGODB", "OP_MSG", start, protoContext);


        for (var filter : getFilters(ProtocolPhase.PRE_CALL, data, out)) {
            if (filter.handle(filterContext, ProtocolPhase.PRE_CALL, data, out)) {
                return out;
            }
        }

        var database = mongoClient.getDatabase(db);

        Document commandResult = database.runCommand(command);
        cmdContainer.setResult(commandResult);


        OpMsgSection section = new OpMsgSection();
        section.getDocuments().add(
                commandResult.toJson(
                        JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()));
        out.getSections().add(section);
        for (var filter : getFilters(ProtocolPhase.POST_CALL, data, out)) {
            if (filter.handle(filterContext, ProtocolPhase.POST_CALL, data, out)) {
                break;
            }
        }
        return out;
    }

    public OpMsgContent runHelloOpMsg(MongoProtoContext protoContext, OpMsgContent lsatOp, Class<?> prevState, MongoClient mongoClient, String dbName) {
        long start = System.currentTimeMillis();

        var out = new OpMsgContent(0, protoContext.getReqResId(), lsatOp.getRequestId());
        var filterContext = new FilterContext("MONGODB", "HELLO_OP_MSG", start, protoContext);

        for (var filter : getFilters(ProtocolPhase.PRE_CALL, lsatOp, out)) {
            if (filter.handle(filterContext, ProtocolPhase.PRE_CALL, lsatOp, out)) {
                return out;
            }
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
        //
        OpMsgSection section = new OpMsgSection();
        section.getDocuments().add(json);
        out.getSections().add(section);
        out.setFlags(0);
        out.setRequestId(protoContext.getReqResId());
        out.setResponseId(lsatOp.getRequestId());
        for (var filter : getFilters(ProtocolPhase.POST_CALL, lsatOp, out)) {
            if (filter.handle(filterContext, ProtocolPhase.POST_CALL, lsatOp, out)) {
                break;
            }
        }
        return out;
    }

    public OpReplyContent runHelloOpQuery(MongoProtoContext protoContext, OpQueryContent lsatOp, Class<?> prevState, MongoClient mongoClient, String dbName) {
        long start = System.currentTimeMillis();
        var out = new OpReplyContent();
        var filterContext = new FilterContext("MONGODB", "HELLO_OP_QUERY", start, protoContext);

        for (var filter : getFilters(ProtocolPhase.PRE_CALL, lsatOp, out)) {
            if (filter.handle(filterContext, ProtocolPhase.PRE_CALL, lsatOp, out)) {
                return out;
            }
        }
//        if (replayer) {
//            var item = storage.read((JsonNode) lsatOp.serialize(), "HELLO_OP_QUERY");
//            var res = new OpReplyContent();
//            res.doDeserialize(item.getOutput(), mapper);
//            res.setRequestId(protoContext.getReqResId());
//            res.setResponseId(lsatOp.getRequestId());
//            res.setFlags(8);
//            return res;
//        }
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
        //var toSend = new OpReplyContent(8, protoContext.getReqResId(), lsatOp.getRequestId());
        //var out2 = new OpReplyContent();
        out.setFlags(8);
        out.setRequestId(protoContext.getReqResId());
        out.setResponseId(lsatOp.getRequestId());
        out.setCursorId(0);
        out.getDocuments().add(json);

        for (var filter : getFilters(ProtocolPhase.POST_CALL, lsatOp, out)) {
            if (filter.handle(filterContext, ProtocolPhase.POST_CALL, lsatOp, out)) {
                break;
            }
        }
        return out;
    }

    public ServerApiVersion getServerApiVersion() {
        return serverApiVersion;
    }
}
