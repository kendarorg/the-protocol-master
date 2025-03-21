package org.kendar.mongo.compressors;


import org.kendar.exceptions.TPMProtocolException;
import org.xerial.snappy.Snappy;

import java.io.IOException;


public class SnappyCompressionHandler implements CompressionHandler {
    @Override
    public byte[] decompress(byte[] bb) {
        try {
            return Snappy.uncompress(bb);
        } catch (IOException e) {
            throw new TPMProtocolException(e);
        }
    }

    @Override
    public int getId() {
        return CompressorIds.SNAPPY;
    }
}
