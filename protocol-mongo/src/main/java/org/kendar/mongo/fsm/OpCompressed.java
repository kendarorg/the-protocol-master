package org.kendar.mongo.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.mongo.compressors.*;
import org.kendar.mongo.fsm.events.CompressedDataEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpCompressed extends MongoState {
    private static final Map<Integer, CompressionHandler> compressionHandlers;
    private static final Logger log = LoggerFactory.getLogger(OpCompressed.class);

    static {
        compressionHandlers = new HashMap<>();
        CompressionHandler ch = new NoopCompressionHandler();
        compressionHandlers.put(ch.getId(), ch);
        ch = new SnappyCompressionHandler();
        compressionHandlers.put(ch.getId(), ch);
        ch = new ZlibCompressionHandler();
        compressionHandlers.put(ch.getId(), ch);
        ch = new ZStdCompressionHandler();
        compressionHandlers.put(ch.getId(), ch);
    }

    public OpCompressed(Class<?>... events) {
        super(events);
    }

    @Override
    protected OpCodes getOpCode() {
        return OpCodes.OP_COMPRESSED;
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, MongoProtoContext protoContext) {
        inputBuffer.setPosition(0);
        int messageLength = inputBuffer.getInt();
        int requestId = inputBuffer.getInt();
        int responseTo = inputBuffer.getInt();
        OpCodes originalOpCode = OpCodes.of(inputBuffer.getInt());
        int uncompressedSize = inputBuffer.getInt();
        byte compressorId = inputBuffer.get();
        int compressedSize = messageLength - (4 + 4 + 4 + 4 + 4 + 4 + 1);
        byte[] decompressedBytes = decompressMessage(inputBuffer.getBytes(compressedSize), compressorId);


        var targetBb = protoContext.buildBuffer();
        targetBb.writeInt(uncompressedSize + 4 + 4 + 4 + 4);
        targetBb.writeInt(requestId);
        targetBb.writeInt(responseTo);
        targetBb.writeInt(originalOpCode.getValue());
        targetBb.write(decompressedBytes);
        targetBb.setPosition(0);
        protoContext.send(new CompressedDataEvent(protoContext, OpCompressed.class, targetBb));
        return iteratorOfEmpty();
    }


    private byte[] decompressMessage(byte[] inputBuffer, byte compressorId) {
        var compressor = compressionHandlers.get((int) compressorId);
        if (compressor == null) {
            log.error("Unknow compression {}", compressorId);
        }
        return compressor.decompress(inputBuffer);
    }
}
