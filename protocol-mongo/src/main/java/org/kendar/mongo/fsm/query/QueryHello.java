package org.kendar.mongo.fsm.query;

import org.kendar.mongo.executor.MongoExecutor;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.fsm.events.OpQueryRequest;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class QueryHello extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(QueryHello.class);
    private static final JsonMapper mapper = new JsonMapper();

    public QueryHello(Class<?>... events) {
        super(events);
    }

    private static Iterator<ProtoStep> getProtoSteps(OpQueryRequest event) {
        MongoProtoContext protoContext = (MongoProtoContext) event.getContext();
        var executor = new MongoExecutor();
        var toSend = executor.runHelloOpQuery(protoContext, event.getData(), event.getPrevState());

        return iteratorOfList(toSend);
    }

    public boolean canRun(OpQueryRequest event) {
        for (var doc : event.getData().getDocuments()) {
            try {
                var jsonTree = mapper.toJsonNode(doc);
                return jsonTree.get("helloOk") != null && jsonTree.get("helloOk").asBoolean();
            } catch (Exception ex) {
                log.trace("Ignorable", ex);
            }
        }
        return false;
    }

    public Iterator<ProtoStep> execute(OpQueryRequest event) {
        try {
            return getProtoSteps(event);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
