package org.kendar.resp3.parser;

import org.junit.jupiter.api.Test;
import org.kendar.redis.parser.Resp3Input;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;
import org.kendar.redis.parser.RespError;

import java.math.BigInteger;

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
    void parseNull() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "_\r\n";
        var result = target.parse(Resp3Input.of(data));
        assertNull(result);
    }

    @Test
    void parseBoolTrue() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "#t\r\n";
        var result = (boolean) target.parse(Resp3Input.of(data));
        assertTrue(result);
    }

    @Test
    void parseBoolFalse() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "#f\r\n";
        var result = (boolean) target.parse(Resp3Input.of(data));
        assertFalse(result);
    }

    @Test
    void parseDouble() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",1.29e-1\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,0.129,0.0001);
    }

    @Test
    void parseDouble1() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",-1.29e-1\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,-0.129,0.0001);
    }



    @Test
    void parseDouble2() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",+1.29\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,1.29,0.0001);
    }

    @Test
    void parseDouble3() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",10\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,10,0.0001);
    }

    @Test
    void parseDoubleInf() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",inf\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,Double.POSITIVE_INFINITY,0.0001);
    }


    @Test
    void parseDoubleMinusInf() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",-inf\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,Double.NEGATIVE_INFINITY,0.0001);
    }


    @Test
    void parseDoubleNan() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = ",nan\r\n";
        var result = (double) target.parse(Resp3Input.of(data));
        assertEquals(result,Float.NaN,0.0001);
    }

    @Test
    void parseBigInteger() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "(3492890328409238509324850943850943825024385\r\n";
        var result = target.parse(Resp3Input.of(data));
        var expected = new BigInteger("3492890328409238509324850943850943825024385");
        assertEquals(result,expected);
    }

    @Test
    void parseBigIntegerNeg() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "(-3492890328409238509324850943850943825024385\r\n";
        var result = target.parse(Resp3Input.of(data));
        var expected = new BigInteger("-3492890328409238509324850943850943825024385");
        assertEquals(result,expected);
    }

    @Test
    void parseBigIntegerPlus() throws Resp3ParseException {
        var target = new Resp3Parser();
        var data = "(+3492890328409238509324850943850943825024385\r\n";
        var result = target.parse(Resp3Input.of(data));
        var expected = new BigInteger("+3492890328409238509324850943850943825024385");
        assertEquals(result,expected);
    }
}
