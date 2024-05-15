package org.kendar.resp3.parser;

import org.junit.jupiter.api.Test;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.redis.parser.RespError;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * According to https://redis.io/docs/latest/develop/reference/protocol-spec
 */
public class ParserTest {
    @Test
    void parseString() throws IOException {
        var target = new Resp3Parser();
        var data = "+OK\r\n";
        var result = target.parse(data);
        assertEquals("OK",result);
    }

    @Test
    void parseError() throws IOException {
        var target = new Resp3Parser();
        var data = "-This is a fancy error\r\n";
        var result = target.parse(data);
        assertTrue(result instanceof RespError);
        assertEquals("This is a fancy error",((RespError)result).getMsg());
        assertEquals("ERR",((RespError)result).getType());
    }

    @Test
    void parseErrorWithERR() throws IOException {
        var target = new Resp3Parser();
        var data = "-ERR unknown command 'asdf'\r\n";
        var result = target.parse(data);
        assertTrue(result instanceof RespError);
        assertEquals("unknown command 'asdf'",((RespError)result).getMsg());
        assertEquals("ERR",((RespError)result).getType());
    }
    @Test
    void parseErrorWithSpecificType() throws IOException {
        var target = new Resp3Parser();
        var data = "-UNKNOWN unknown command 'asdf'\r\n";
        var result = target.parse(data);
        assertTrue(result instanceof RespError);
        assertEquals("unknown command 'asdf'",((RespError)result).getMsg());
        assertEquals("UNKNOWN",((RespError)result).getType());
    }

    @Test
    void parseInt() throws IOException {
        var target = new Resp3Parser();
        var data = ":22\r\n";
        var result = (int)target.parse(data);
        assertEquals(22,result);
    }
    @Test
    void parseIntPlus() throws IOException {
        var target = new Resp3Parser();
        var data = ":+22\r\n";
        var result = (int)target.parse(data);
        assertEquals(22,result);
    }
    @Test
    void parseIntMinus() throws IOException {
        var target = new Resp3Parser();
        var data = ":-22\r\n";
        var result = (int)target.parse(data);
        assertEquals(-22,result);
    }
}
