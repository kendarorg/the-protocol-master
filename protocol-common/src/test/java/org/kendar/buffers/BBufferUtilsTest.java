package org.kendar.buffers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BBufferUtilsTest {
    @Test
    void setAndGetBytes() {
        var mainAr = new byte[]{0x01, 0x01};
        BBufferUtils.setBit(mainAr, 9);
        BBufferUtils.unSetBit(mainAr, 8);
        assertEquals(1, BBufferUtils.getBit(mainAr, 0));
        assertEquals(0, BBufferUtils.getBit(mainAr, 1));
        assertEquals(0, BBufferUtils.getBit(mainAr, 2));
        assertEquals(0, BBufferUtils.getBit(mainAr, 3));
        assertEquals(0, BBufferUtils.getBit(mainAr, 4));
        assertEquals(0, BBufferUtils.getBit(mainAr, 5));
        assertEquals(0, BBufferUtils.getBit(mainAr, 6));
        assertEquals(0, BBufferUtils.getBit(mainAr, 7));
        assertEquals(0, BBufferUtils.getBit(mainAr, 8));
        assertEquals(1, BBufferUtils.getBit(mainAr, 9));
        assertEquals(0, BBufferUtils.getBit(mainAr, 10));
        assertEquals(0, BBufferUtils.getBit(mainAr, 11));
        assertEquals(0, BBufferUtils.getBit(mainAr, 12));
        assertEquals(0, BBufferUtils.getBit(mainAr, 13));
        assertEquals(0, BBufferUtils.getBit(mainAr, 14));
        assertEquals(0, BBufferUtils.getBit(mainAr, 15));
    }
}
