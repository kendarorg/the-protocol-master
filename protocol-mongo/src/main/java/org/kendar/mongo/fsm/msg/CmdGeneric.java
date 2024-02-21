package org.kendar.mongo.fsm.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kendar.mongo.executor.MongoExecutor;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.fsm.StandardOpMsgCommand;
import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class CmdGeneric extends StandardOpMsgCommand {
    private static final ObjectMapper mapper = new ObjectMapper();

    public CmdGeneric(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean canRun(String identifier, String doc) {
        try {
            var jsonTree = mapper.readTree(doc);
            return jsonTree.get("saslStart") == null
                    && jsonTree.get("saslContinue") == null
                    && jsonTree.get("hello") == null
                    && jsonTree.get("helloOk") == null;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    protected Iterator<ProtoStep> executeInternal(OpMsgRequest event) {
        try {
            MongoProtoContext protoContext = (MongoProtoContext) event.getContext();
            var executor = new MongoExecutor(protoContext.getProxy());

            var toSend = executor.runGenericOpMsg(protoContext, event.getData(), event.getPrevState());

            return iteratorOfList(toSend);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


}
