package org.kendar.mongo.fsm;

import com.mongodb.MongoClientSettings;
import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.kendar.buffers.BBuffer;
import org.kendar.mongo.dtos.OpQueryContent;
import org.kendar.mongo.fsm.events.OpQueryRequest;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;

public class OpQuery extends StandardMessage {
    public OpQuery(Class<?>... events) {
        super(events);
    }

    private static String parseBsonDocument(CodecRegistry codecRegistry, ByteBuffer byteBuffer) {
        BsonDocumentCodec documentCodec = new BsonDocumentCodec(codecRegistry);
        BsonBinaryReader bsonReader = new BsonBinaryReader(byteBuffer);
        BsonDocument document = documentCodec.decode(bsonReader, DecoderContext.builder().build());
        var json = document.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build());
        System.out.println(json);
        return json;
    }

    @Override
    protected OpCodes getOpCode() {
        return OpCodes.OP_QUERY;
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, ProtoContext protoContext) {
        var context = (MongoProtoContext) protoContext;
        inputBuffer.setPosition(0);
        var length = inputBuffer.getInt();//length
        var requestId = inputBuffer.getInt();
        var responseId = inputBuffer.getInt();
        var opCode = OpCodes.of(inputBuffer.getInt());//opcode
        var flagBits = inputBuffer.getInt();
        var fullCollectionName = inputBuffer.getUtf8String();
        var numberToSkip = inputBuffer.getInt();
        var numberToReturn = inputBuffer.getInt();
        var docs = new ArrayList<String>();

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry());
        var byteBuffer = ByteBuffer.
                wrap(inputBuffer.getBytes(inputBuffer.size() - inputBuffer.getPosition())).
                order(ByteOrder.LITTLE_ENDIAN);
        while (byteBuffer.hasRemaining()) {
            docs.add(parseBsonDocument(codecRegistry, byteBuffer));
        }
        protoContext.send(new OpQueryRequest(protoContext, OpQuery.class, new OpQueryContent(
                flagBits, requestId, responseId, fullCollectionName, numberToSkip, numberToReturn, docs)));
        return iteratorOfEmpty();
    }
}
