package org.kendar.mongo.fsm.msg;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.mongo.dtos.OpMsgSection;
import org.kendar.mongo.executor.MongoExecutor;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.mongo.fsm.StandardOpMsgCommand;
import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.utils.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class CmdSaslContinue extends StandardOpMsgCommand {

    protected static final JsonMapper mapper = new JsonMapper();
    private static final String GS2_HEADER = "n,,";
    private static final int MINIMUM_ITERATION_COUNT = 4096;

    public CmdSaslContinue(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean canRun(String identifier, String doc) {
        try {
            var jsonTree = mapper.toJsonNode(doc);
            return jsonTree.get("saslStart") != null;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    protected Iterator<ProtoStep> executeInternal(OpMsgRequest event) {

        try {
            var login = "";
            var nonce = "";
            MongoProtoContext protoContext = (MongoProtoContext) event.getContext();
            var executor = new MongoExecutor();

            var bd = BsonDocument.parse(event.getData().getSections().get(0).getDocuments().get(0));
            var binaryData = bd.getBinary("payload").getData();
            var pl = new String(binaryData);
            var spl = pl.split(",");
            for (var sl : spl) {
                var slspl = sl.split("=");
                if (slspl.length == 2 && slspl[0].equalsIgnoreCase("n")) {
                    login = slspl[1];
                } else if (slspl.length == 2 && slspl[0].equalsIgnoreCase("r")) {
                    nonce = slspl[1];
                }
            }

            var newPayload =// GS2_HEADER +
                    "r=" + nonce + "," +
                            "s=QUI=," +
                            "i=" + MINIMUM_ITERATION_COUNT;

            var resultMap = new Document();
            var convid = protoContext.getReqResId();
            resultMap.put("conversationId", convid);
            resultMap.put("done", true);
            resultMap.put("payload", newPayload.getBytes(StandardCharsets.UTF_8));
            resultMap.put("ok", true);

            protoContext.setValue("CONVERSATION_ID", convid);

            var json = resultMap.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build());
            var toSend = new OpMsgContent(0, protoContext.getReqResId(), event.getData().getRequestId());
            OpMsgSection section = new OpMsgSection();
            section.getDocuments().add(json);
            toSend.getSections().add(section);
            return iteratorOfList(toSend);
        } catch (Exception ex) {
            throw new TPMProtocolException(ex);
        }
    }
}
