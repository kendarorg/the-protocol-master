package org.kendar.mongo.compressors;

public interface CompressionHandler {
    byte[] decompress(byte[] bb);

    int getId();
}
