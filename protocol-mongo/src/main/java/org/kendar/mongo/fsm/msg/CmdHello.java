package org.kendar.mongo.fsm.msg;


import org.kendar.exceptions.TPMProtocolException;
import org.kendar.mongo.executor.MongoExecutor;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.fsm.StandardOpMsgCommand;
import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class CmdHello extends StandardOpMsgCommand {
    protected static final JsonMapper mapper = new JsonMapper();

    public CmdHello(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean canRun(String identifier, String doc) {
        try {
            var jsonTree = mapper.toJsonNode(doc);
            return jsonTree.get("saslStart") == null
                    && jsonTree.get("saslContinue") == null
                    && jsonTree.get("helloOk") != null
                    && jsonTree.get("hello") != null;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    protected Iterator<ProtoStep> executeInternal(OpMsgRequest event) {
        try {
            MongoProtoContext protoContext = (MongoProtoContext) event.getContext();
            var executor = new MongoExecutor();
            var toSend = executor.runHelloOpMsg(protoContext, event.getData(), event.getPrevState());

            return iteratorOfList(toSend);
        } catch (Exception ex) {
            throw new TPMProtocolException(ex);
        }
    }
}
