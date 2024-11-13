package org.kendar.http.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Md5TesterTest {
    @Test
    void testMd5String() {
        var result = Md5Tester.calculateMd5("test");
        assertEquals("98f6bcd4621d373cade4e832627b4f6", result);
    }

    @Test
    void testMd5Byte() {
        var result = Md5Tester.calculateMd5(new byte[]{1, 2, 3, 4, 5});
        assertEquals("7cfdd07889b3295d6a550914ab35e068", result);
    }

    @Test
    void testMd5Null() {
        var result = Md5Tester.calculateMd5(null);
        assertEquals("0", result);
    }
}
