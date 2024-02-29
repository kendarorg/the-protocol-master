package org.kendar.buffers;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class BBEndiannessConverterTest {
    @Test
    void shortValue() {
        final int LEN = 2;
        var dst = new byte[LEN];
        var verifyBe = new byte[]{100, 126};
        var buffer = ByteBuffer.allocate(LEN);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 25726);
        buffer.flip();
        buffer.get(dst, 0, LEN);
        assertArrayEquals(verifyBe, dst);

        var verifyLe = BBEndiannessConverter.swap2Bytes(verifyBe);

        buffer = ByteBuffer.allocate(LEN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 25726);
        buffer.flip();
        buffer.get(dst, 0, LEN);
        assertArrayEquals(verifyLe, dst);
    }

    @Test
    void intValue() {
        final int LEN = 4;
        var dst = new byte[LEN];
        var verifyBe = new byte[]{73, -106, 2, -46};
        var buffer = ByteBuffer.allocate(LEN);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(1234567890);
        buffer.flip();
        buffer.get(dst, 0, LEN);
        assertArrayEquals(verifyBe, dst);

        var verifyLe = BBEndiannessConverter.swap4Bytes(verifyBe);

        buffer = ByteBuffer.allocate(LEN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(1234567890);
        buffer.flip();
        buffer.get(dst, 0, LEN);
        assertArrayEquals(verifyLe, dst);
    }


    @Test
    void longValue() {
        final int LEN = 8;
        var dst = new byte[LEN];
        var verifyBe = new byte[]{17, 34, 16, -12, -105, 20, 14, 20};
        var buffer = ByteBuffer.allocate(LEN);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(1234567890545675796L);
        buffer.flip();
        buffer.get(dst, 0, LEN);
        assertArrayEquals(verifyBe, dst);

        var verifyLe = BBEndiannessConverter.swap8Bytes(verifyBe);

        buffer = ByteBuffer.allocate(LEN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(1234567890545675796L);
        buffer.flip();
        buffer.get(dst, 0, LEN);
        assertArrayEquals(verifyLe, dst);
    }
}
