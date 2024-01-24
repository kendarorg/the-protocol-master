package org.kendar.mongo.compressors;

public class NoopCompressionHandler implements CompressionHandler {
    @Override
    public byte[] decompress(byte[] bb) {
        return bb;
    }

    @Override
    public int getId() {
        return CompressorIds.NOOP;
    }
}
