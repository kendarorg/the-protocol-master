package org.kendar.resp3.parser;

import org.junit.jupiter.api.Test;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.redis.parser.RespError;
import org.kendar.redis.parser.RespPush;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionsParserTest {


    @Test
    void parseNullArray() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*-1\r\n";
        var result = target.parse(data);
        assertNull(result);
    }

    @Test
    void parseEmptyArray() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*0\r\n";
        var result = target.parse(data);
        assertInstanceOf(List.class, result);
        assertEquals(0, ((List) result).size());
    }

    @Test
    void parseExampleArray1() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n";
        var result = target.parse(data);
        assertInstanceOf(List.class, result);
        assertEquals(2, ((List) result).size());
        assertEquals("hello", ((List) result).get(0));
        assertEquals("world", ((List) result).get(1));
    }

    @Test
    void parseExampleArray2() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*3\r\n:1\r\n:-2\r\n:+3\r\n";
        var result = target.parse(data);
        assertInstanceOf(List.class, result);
        assertEquals(3, ((List) result).size());
        assertEquals(1, ((List) result).get(0));
        assertEquals(-2, ((List) result).get(1));
        assertEquals(3, ((List) result).get(2));
    }

    @Test
    void parseExampleArrayNested() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Hello\r\n-World\r\n";
        var result = target.parse(data);
        assertInstanceOf(List.class, result);
        assertEquals(2, ((List) result).size());
        var listZero = (List) ((List) result).get(0);
        var listOne = (List) ((List) result).get(1);
        assertEquals(1, listZero.get(0));
        assertEquals(2, listZero.get(1));
        assertEquals(3, listZero.get(2));
        assertEquals("Hello", listOne.get(0));
        var respError = (RespError) listOne.get(1);
        assertEquals("ERR", respError.getType());
        assertEquals("World", respError.getMsg());
    }

    @Test
    void parseExampleArrayWithNull() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*3\r\n$5\r\nhello\r\n$-1\r\n$5\r\nworld\r\n";
        var result = target.parse(data);
        assertInstanceOf(List.class, result);
        assertEquals(3, ((List) result).size());
        assertEquals("hello", ((List) result).get(0));
        assertNull(((List) result).get(1));
        assertEquals("world", ((List) result).get(2));
    }

    @Test
    void map() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "%2\r\n+first\r\n:1\r\n+second\r\n:2\r\n";
        var result = (List<Object>) target.parse(data);
        assertEquals(3, result.size());
        assertEquals("@@MAP@@", result.get(0));
        assertEquals("first", ((List<Object>) result.get(1)).get(0));
        assertEquals(1, ((List<Object>) result.get(1)).get(1));
        assertEquals("second", ((List<Object>) result.get(2)).get(0));
        assertEquals(2, ((List<Object>) result.get(2)).get(1));
    }

    @Test
    void set() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "~3\r\n+first\r\n+second\r\n:2\r\n";
        var result = (List<Object>) target.parse(data);
        assertEquals(4, result.size());
        assertEquals("@@SET@@", result.get(0));
        assertEquals("first", result.get(1));
        assertEquals("second", result.get(2));
        assertEquals(2, result.get(3));
    }

    @Test
    void push() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ">4\r\n+first\r\n:1\r\n+second\r\n:2\r\n";
        var result = (RespPush) target.parse(data);
        assertEquals(5, result.size());
        assertEquals("@@PUSH@@", result.get(0));
        assertEquals("first", result.get(1));
        assertEquals(1, result.get(2));
        assertEquals("second", result.get(3));
        assertEquals(2, result.get(4));
    }
}
