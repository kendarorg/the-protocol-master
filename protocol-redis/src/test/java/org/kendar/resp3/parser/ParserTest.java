package org.kendar.resp3.parser;

import org.junit.jupiter.api.Test;
import org.kendar.redis.parser.Resp3Input;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.redis.parser.RespError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * According to https://redis.io/docs/latest/develop/reference/protocol-spec
 */
public class ParserTest {
    @Test
    void parseString() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "+OK\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertEquals("OK",result);
    }

    @Test
    void parseError() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "-This is a fancy error\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof RespError);
        assertEquals("This is a fancy error",((RespError)result).getMsg());
        assertEquals("ERR",((RespError)result).getType());
    }

    @Test
    void parseErrorWithERR() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "-ERR unknown command 'asdf'\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof RespError);
        assertEquals("unknown command 'asdf'",((RespError)result).getMsg());
        assertEquals("ERR",((RespError)result).getType());
    }
    @Test
    void parseErrorWithSpecificType() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "-UNKNOWN unknown command 'asdf'\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertTrue(result instanceof RespError);
        assertEquals("unknown command 'asdf'",((RespError)result).getMsg());
        assertEquals("UNKNOWN",((RespError)result).getType());
    }

    @Test
    void parseInt() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ":22\r\n";
        var result = (int)target.parse(Resp3Input.of(data));
        assertEquals(22,result);
    }
    @Test
    void parseIntPlus() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ":+22\r\n";
        var result = (int)target.parse(Resp3Input.of(data));
        assertEquals(22,result);
    }
    @Test
    void parseIntMinus() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ":-22\r\n";
        var result = (int)target.parse(Resp3Input.of(data));
        assertEquals(-22,result);
    }



    @Test
    void parseBulk() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "$5\r\nhello\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertEquals("hello",result);
    }

    @Test
    void parseBulkNotMatchingExpected() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "$4\r\nhello\r\n";
        var isMissingData = true;
        try{
            target.parse(Resp3Input.of(data));
        }catch (Resp3ParseException ex){
            isMissingData = ex.isMissingData();
        }
        assertFalse(isMissingData);
    }

    @Test
    void parseBulkRequireMoreData() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "$6\r\nhello\r\n";
        var isMissingData = false;
        try{
            target.parse(Resp3Input.of(data));
        }catch (Resp3ParseException ex){
            isMissingData = ex.isMissingData();
        }
        assertTrue(isMissingData);
    }

    @Test
    void parseBulkEmpty() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "$0\r\n\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertEquals("",result);
    }

    @Test
    void parseBulkNull() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "$-1\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertNull(result);
    }

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

    @Test
    void parseNull() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "_\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertNull(result);
    }
}
