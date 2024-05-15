package org.kendar.resp3.parser;

import org.junit.jupiter.api.Test;
import org.kendar.redis.parser.Resp3Input;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.redis.parser.RespError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionsParserTest {


    @Test
    void parseNullArray() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*-1\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertNull(result);
    }

    @Test
    void parseEmptyArray() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*0\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof List);
        assertEquals(0,((List)result).size());
    }

    @Test
    void parseExampleArray1() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof List);
        assertEquals(2,((List)result).size());
        assertEquals("hello",((List)result).get(0));
        assertEquals("world",((List)result).get(1));
    }

    @Test
    void parseExampleArray2() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*3\r\n:1\r\n:-2\r\n:+3\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof List);
        assertEquals(3,((List)result).size());
        assertEquals(1,((List)result).get(0));
        assertEquals(-2,((List)result).get(1));
        assertEquals(3,((List)result).get(2));
    }

    @Test
    void parseExampleArrayNested() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Hello\r\n-World\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof List);
        assertEquals(2,((List)result).size());
        var listZero = (List)((List)result).get(0);
        var listOne = (List)((List)result).get(1);
        assertEquals(1,((List)listZero).get(0));
        assertEquals(2,((List)listZero).get(1));
        assertEquals(3,((List)listZero).get(2));
        assertEquals("Hello",((List)listOne).get(0));
        var respError = (RespError)((List)listOne).get(1);
        assertEquals("ERR",respError.getType());
        assertEquals("World",respError.getMsg());
    }

    @Test
    void parseExampleArrayWithNull() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*3\r\n$5\r\nhello\r\n$-1\r\n$5\r\nworld\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof List);
        assertEquals(3,((List)result).size());
        assertEquals("hello",((List)result).get(0));
        assertNull(((List)result).get(1));
        assertEquals("world",((List)result).get(2));
    }
}
