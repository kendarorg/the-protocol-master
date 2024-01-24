package org.kendar.mongo.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import org.kendar.mongo.MongoProxy;
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.mongo.dtos.OpQueryContent;
import org.kendar.mongo.dtos.OpReplyContent;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;

public class MongoExecutor {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Proxy proxy;

    public MongoExecutor(Proxy proxy) {

        this.proxy = proxy;
    }

    private String getDb(OpMsgContent data) {

        try {
            for (var section : data.getSections()) {
                for (var doc : section.getDocuments()) {
                    var jsonTree = mapper.readTree(doc);
                    var db = (JsonNode) jsonTree.get("$db");
                    return db.asText();
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Missing db on OpMsg");
        }
    }

    public OpMsgContent runGenericOpMsg(MongoProtoContext protoContext, OpMsgContent data, Class<?> prevState) {
        var mongoClient = ((MongoClient) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
        var mongoProxy = (MongoProxy) protoContext.getProxy();
        return mongoProxy.runGenericOpMsg(protoContext, data, prevState, mongoClient, getDb(data));

    }

    public OpMsgContent runHelloOpMsg(MongoProtoContext protoContext, OpMsgContent lsatOp, Class<?> prevState) throws JsonProcessingException {

        var mongoClient = ((MongoClient) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
        var mongoProxy = (MongoProxy) protoContext.getProxy();
        return mongoProxy.runHelloOpMsg(protoContext, lsatOp, prevState, mongoClient, getDb(lsatOp));

    }

    public OpReplyContent runHelloOpQuery(MongoProtoContext protoContext, OpQueryContent lsatOp, Class<?> prevState) {
        var mongoClient = ((MongoClient) ((ProxyConnection) protoContext.getValue("CONNECTION")).getConnection());
        var mongoProxy = (MongoProxy) protoContext.getProxy();
        return mongoProxy.runHelloOpQuery(protoContext, lsatOp, prevState, mongoClient, "admin");
    }
}
