package org.kendar.resp3.parser;

import org.junit.jupiter.api.Test;
import org.kendar.redis.parser.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertTrue(result instanceof List);
        assertEquals(0,((List)result).size());
    }

    @Test
    void parseExampleArray1() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n";
        var result = target.parse(data);
        assertTrue(result instanceof List);
        assertEquals(2,((List)result).size());
        assertEquals("hello",((List)result).get(0));
        assertEquals("world",((List)result).get(1));
    }

    @Test
    void parseExampleArray2() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "*3\r\n:1\r\n:-2\r\n:+3\r\n";
        var result = target.parse(data);
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
        var result = target.parse(data);
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
        var result = target.parse(data);
        assertTrue(result instanceof List);
        assertEquals(3,((List)result).size());
        assertEquals("hello",((List)result).get(0));
        assertNull(((List)result).get(1));
        assertEquals("world",((List)result).get(2));
    }

    @Test
    void map() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "%2\r\n+first\r\n:1\r\n+second\r\n:2\r\n";
        var result = (Map<Object,Object>)target.parse(data);
        assertEquals(2,result.size());
        assertTrue(result.containsKey("first"));
        assertEquals(1,result.get("first"));
        assertTrue(result.containsKey("second"));
        assertEquals(2,result.get("second"));
    }

    @Test
    void set() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "~3\r\n+first\r\n+second\r\n:2\r\n";
        var result = (Set<Object>)target.parse(data);
        assertEquals(3,result.size());
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
        assertTrue(result.contains(2));
    }

    @Test
    void push() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ">4\r\n+first\r\n:1\r\n+second\r\n:2\r\n";
        var result = (RespPush)target.parse(data);
        assertEquals(4,result.size());
        assertEquals("first",result.get(0));
        assertEquals(1,result.get(1));
        assertEquals("second",result.get(2));
        assertEquals(2,result.get(3));
    }
}
