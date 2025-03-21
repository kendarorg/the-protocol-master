package org.kendar.mongo.compressors;


import org.kendar.exceptions.TPMProtocolException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;


public class ZlibCompressionHandler implements CompressionHandler {
    @Override
    public byte[] decompress(byte[] bb) {
        try {
            InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(bb));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inflaterInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new TPMProtocolException(e);
        }
    }

    @Override
    public int getId() {
        return CompressorIds.ZLIB;
    }
}
