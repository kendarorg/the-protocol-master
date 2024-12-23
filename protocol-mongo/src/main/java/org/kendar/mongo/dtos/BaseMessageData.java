package org.kendar.mongo.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClientSettings;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
import org.kendar.buffers.BBuffer;
import org.kendar.mongo.fsm.OpCodes;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.utils.JsonMapper;

import java.util.HashMap;

public abstract class BaseMessageData implements NetworkReturnMessage {
    protected static final JsonMapper mapper = new JsonMapper();
    private int flags;
    private int requestId;
    private int responseId;
    private OpCodes opCode;

    public BaseMessageData() {

    }

    public BaseMessageData(int flags, int requestId, int responseId, OpCodes opCode) {
        this.flags = flags;
        this.requestId = requestId;
        this.responseId = responseId;
        this.opCode = opCode;
    }


    public static byte[] toBytes(BsonDocument document) {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry());
        BsonDocumentCodec documentCodec = new BsonDocumentCodec(codecRegistry);

        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        documentCodec.encode(writer, document, EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        return buffer.toByteArray();
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public OpCodes getOpCode() {
        return opCode;
    }

    public void setOpCode(OpCodes opCode) {
        this.opCode = opCode;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getResponseId() {
        return responseId;
    }

    public void setResponseId(int responseId) {
        this.responseId = responseId;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.writeInt(9999);
        resultBuffer.writeInt(requestId);
        resultBuffer.writeInt(responseId);
        resultBuffer.writeInt(opCode.getValue());
        resultBuffer.writeInt(flags);
    }


    public Object serialize() {
        var dataMap = new HashMap<String, Object>();
        dataMap.put("opCode", getOpCode().toString());
        dataMap.put("flags", getFlags());
        serialize(dataMap, mapper);
        return mapper.toJsonNode(mapper.serialize(dataMap));
    }


    public void doDeserialize(JsonNode toDeserialize, JsonMapper mapper) {
        opCode = OpCodes.valueOf(toDeserialize.get("opCode").textValue());
        flags = toDeserialize.get("flags").asInt();
    }

    protected abstract void serialize(HashMap<String, Object> dataMap, JsonMapper mapper);
}
