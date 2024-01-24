package org.kendar.mongo.compressors;

import com.github.luben.zstd.ZstdInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class ZStdCompressionHandler implements CompressionHandler {
    @Override
    public byte[] decompress(byte[] bb) {
        try {
            ZstdInputStream zstdInputStream = new ZstdInputStream(new ByteArrayInputStream(bb));
            ByteArrayOutputStream zstdOutputStream = new ByteArrayOutputStream();

            byte[] zstdBuffer = new byte[4096];
            int zstdBytesRead;
            while ((zstdBytesRead = zstdInputStream.read(zstdBuffer)) != -1) {
                zstdOutputStream.write(zstdBuffer, 0, zstdBytesRead);
            }

            return zstdOutputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getId() {
        return CompressorIds.Z_STD;
    }
}
