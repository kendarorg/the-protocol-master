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
import java.util.*;


public class CmdSaslStart extends StandardOpMsgCommand {

    protected static final JsonMapper mapper = new JsonMapper();
    private static final String GS2_HEADER = "n,,";
    private static final int MINIMUM_ITERATION_COUNT = 4096;

    public CmdSaslStart(Class<?>... events) {
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

    public static List<String> readNullTerminatedStrings(byte[] data) {
        var result = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                if (i > start) { // avoid empty trailing \0
                    String s = new String(data, start, i - start, StandardCharsets.UTF_8);
                    result.add(s);
                }
                start = i + 1;
            }
        }

        return result;
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
            var mechanism = bd.getString("mechanism").getValue();

            switch (mechanism) {
                case "SCRAM-SHA-256":
                case "SCRAM-SHA-1":
                    return handleScramAuthentication(event, binaryData, protoContext, nonce);
                case "MONGODB-X509":
                    return handleX509Authentication(event, binaryData, protoContext, nonce);
                case "PLAIN":
                    return handlePlainAuthentication(event, binaryData, protoContext, nonce);
                case "GSSAPI":
                    return handleGssApiAuthentication(event, binaryData, protoContext, nonce);
                default:
                    throw new TPMProtocolException("Unsupported mechanism: " + mechanism);
            }
        } catch (Exception ex) {
            throw new TPMProtocolException(ex);
        }
    }

    private Iterator<ProtoStep> handleScramAuthentication(OpMsgRequest event, byte[] binaryData, MongoProtoContext protoContext, String nonce) {
        return null;
    }

    private Iterator<ProtoStep> handleGssApiAuthentication(OpMsgRequest event, byte[] binaryData, MongoProtoContext protoContext, String nonce) {
        var toSend = generateSuccessMessage(event, protoContext, nonce);
        return iteratorOfList(toSend);
    }

    private Iterator<ProtoStep> handleX509Authentication(OpMsgRequest event, byte[] binaryData, MongoProtoContext protoContext, String nonce) {
        var toSend = generateSuccessMessage(event, protoContext, nonce);
        return iteratorOfList(toSend);
    }

    private static Iterator<ProtoStep> handlePlainAuthentication(OpMsgRequest event, byte[] binaryData, MongoProtoContext protoContext, String nonce) {
        var result = readNullTerminatedStrings(binaryData);
        if(result.size()==3) {
            protoContext.setValue("userid", result.get(0));
            protoContext.setValue("password", result.get(2));
        }
        var toSend = generateSuccessMessage(event, protoContext, nonce);
        return iteratorOfList(toSend);
    }

    private static OpMsgContent generateSuccessMessage(OpMsgRequest event, MongoProtoContext protoContext, String nonce) {
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
        return toSend;
    }
}
