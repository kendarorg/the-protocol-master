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
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.mongo.dtos.OpMsgSection;
import org.kendar.mongo.fsm.events.OpMsgRequest;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

public class OpMsg extends StandardMessage {
    public OpMsg(Class<?>... events) {
        super(events);
    }

    private static String parseBsonDocument(CodecRegistry codecRegistry, ByteBuffer byteBuffer) {
        BsonDocumentCodec documentCodec = new BsonDocumentCodec(codecRegistry);
        BsonBinaryReader bsonReader = new BsonBinaryReader(byteBuffer);
        BsonDocument document = documentCodec.decode(bsonReader, DecoderContext.builder().build());
        var json = document.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build());
        System.out.println("\t\t" + json);
        return json;
    }

    @Override
    protected OpCodes getOpCode() {
        return OpCodes.OP_MSG;
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
        var newData = new OpMsgContent(flagBits, requestId, responseId);
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry());
        while (inputBuffer.getPosition() < inputBuffer.size()) {
            var payloadType = inputBuffer.get();
            if (payloadType == 0) {
                var section = new OpMsgSection();
                System.out.println("\tSection 0");
                var documentLength = inputBuffer.getInt(inputBuffer.getPosition());
                inputBuffer.setPosition(inputBuffer.getPosition());
                var byteBuffer = ByteBuffer.
                        wrap(inputBuffer.getBytes(documentLength)).
                        order(ByteOrder.LITTLE_ENDIAN);
                section.getDocuments().add(parseBsonDocument(codecRegistry, byteBuffer));
                newData.getSections().add(section);
            } else if (payloadType == 1) {
                var section = new OpMsgSection();
                var sequenceSize = inputBuffer.getInt();
                section.setIdentifier(inputBuffer.getUtf8String());
                System.out.println("\tSection 1");
                System.out.println("\t\tIdentifier: " + section.getIdentifier());
                var byteBuffer = ByteBuffer.
                        wrap(inputBuffer.getBytes(sequenceSize - 4 - section.getIdentifier().length() - 1)).
                        order(ByteOrder.LITTLE_ENDIAN);
                while (byteBuffer.hasRemaining()) {
                    section.getDocuments().add(parseBsonDocument(codecRegistry, byteBuffer));
                }
                newData.getSections().add(section);

            } else {
                throw new RuntimeException("INVALID PAYLOAD TYPE " + payloadType);
            }
        }

        protoContext.send(new OpMsgRequest(protoContext, OpMsg.class, newData));

        return iteratorOfEmpty();
    }
}
