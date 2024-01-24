package org.kendar.buffers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BBufferArraysTest {
    byte[] ba(int start, int length) {
        var ba = new byte[length];

        for (var i = 0; i < ba.length; i++) {
            ba[i] = (byte) (i + start);
        }
        return ba;
    }

    @Test
    void simpleArray() {
        var target = new BBuffer();
        var expected = ba(0, 10);
        target.write(expected);
        assertEquals(10, target.size());
        var data = target.getBytes(0, 10);
        assertArrayEquals(expected, data);
    }

    @Test
    void simpleSubArray() {
        var target = new BBuffer();
        var expected = ba(5, 3);
        target.write(ba(0, 10));
        assertEquals(10, target.size());
        var data = target.getBytes(5, 3);
        assertArrayEquals(expected, data);
    }

    @Test
    void simpleSubArrayLeft() {
        var target = new BBuffer();
        var expected = ba(0, 2);
        target.write(ba(0, 10));
        assertEquals(10, target.size());
        var data = target.getBytes(0, 2);
        assertArrayEquals(expected, data);
    }

    @Test
    void simpleSubArrayRight() {
        var target = new BBuffer();
        var expected = ba(8, 2);
        target.write(ba(0, 10));
        assertEquals(10, target.size());
        var data = target.getBytes(8, 2);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleArray() {
        var target = new BBuffer();
        var expected = ba(0, 20);
        target.write(ba(0, 10));
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(0, 20);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleSubArray() {
        var target = new BBuffer();
        var expected = ba(5, 10);
        target.write(ba(0, 10));
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(5, 10);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleSubArrayLeft() {
        var target = new BBuffer();
        var expected = ba(0, 11);
        target.write(ba(0, 10));
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(0, 11);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleSubArrayRight() {
        var target = new BBuffer();
        var expected = ba(9, 11);
        target.write(ba(0, 10));
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(9, 11);
        assertArrayEquals(expected, data);
    }

    @Test
    void simpleArrayMore() {
        var target = new BBuffer();
        var expected = ba(5, 5);
        target.write(ba(0, 10));
        assertEquals(10, target.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> target.getBytes(5, 10));
    }

    @Test
    void zeroLength() {
        var target = new BBuffer();
        target.write(new byte[0]);
        assertEquals(0, target.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> target.getBytes(0, 10));
    }


    @Test
    void doubleArrayAndZero() {
        var target = new BBuffer();
        var expected = ba(0, 20);
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(0, 20);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleSubArrayAndZero() {
        var target = new BBuffer();
        var expected = ba(5, 10);
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(5, 10);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleSubArrayLeftAndZero() {
        var target = new BBuffer();
        var expected = ba(0, 11);
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(0, 11);
        assertArrayEquals(expected, data);
    }

    @Test
    void doubleSubArrayRightAndZero() {
        var target = new BBuffer();
        var expected = ba(9, 11);
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));
        assertEquals(20, target.size());
        var data = target.getBytes(9, 11);
        assertArrayEquals(expected, data);
    }

    @Test
    void readSingleBytesTillEnd() {
        var target = new BBuffer();
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));

        target.resetPosition();
        for (var i = 0; i < target.size(); i++) {
            var value = target.get();
            assertEquals((byte) i, value);
        }
    }

    @Test
    void readMultiBytesTillEnd() {
        var target = new BBuffer();
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));

        target.resetPosition();
        for (var i = 0; i < target.size() / 2; i++) {
            var data = target.getBytes(2);
            var expected = new byte[]{(byte) (i * 2), (byte) (i * 2 + 1)};
            assertArrayEquals(expected, data);
        }
    }

    @Test
    void truncateArray() {
        var target = new BBuffer();
        target.write(ba(0, 10));
        target.write(new byte[0]);
        target.write(ba(10, 10));

        target.resetPosition();
        target.getBytes(2);
        target.get();
        target.getBytes(1);

        target.truncate();
        assertArrayEquals(ba(4, 16), target.toArray());
    }
}
