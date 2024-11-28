package org.kendar.sql;

import org.junit.jupiter.api.Test;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.SqlStringType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComplexSqlStringParserTest {

    final static String SP_EXAMPLE = "create or replace procedure transfer(\n" +
            "   sender int,\n" +
            "   receiver int, \n" +
            "   amount dec\n" +
            ")\n" +
            "language plpgsql    \n" +
            "as $$\n" +
            //"as XX\n" +
            "begin\n" +
            "    -- subtracting the amount from the sender's account \n" +
            "    update accounts 'test' \n" +
            "    set balance = balance - amount \n" +
            "    where id = sender;\n" +
            "\n" +
            "    -- adding the amount to the receiver's account\n" +
            "    update accounts \n" +
            "    set balance = balance + amount \n" +
            "    where id = receiver;\n" +
            "\n" +
            "    commit;\n" +
            "end;$$";

    @Test
    void testComments() {
        var query = "test0 --aaaaaa\n" +
                "test1 #bb bbbb\n" +
                "test2 /* cccc ddd */ test3\n" +
                "test4 /* eee\n" +
                "ffff */ test5";
        var target = new SqlStringParser("$");
        var result = target.parseString(query);
        assertEquals(5, result.size());

    }

    @Test
    void testSpCreationParsingTypes() {
        var sp = "SELECT * FROM FUFFA;" + SP_EXAMPLE;
        var target = new SqlStringParser("$");
        var result = target.getTypes(sp);
        assertEquals(2, result.size());
        assertEquals(SqlStringType.SELECT, result.get(0).getType());
        assertEquals(SqlStringType.UPDATE, result.get(1).getType());
    }


    @Test
    void testSpCreationParsingSql() {
        var sp = "SELECT * FROM FUFFA;" + SP_EXAMPLE;
        var target = new SqlStringParser("$");
        var result = target.parseSql(sp);
        assertEquals(2, result.size());
        assertEquals("SELECT * FROM FUFFA;", result.get(0));
    }

    @Test
    void testSpCreationParsing() {
        var sp = SP_EXAMPLE;
        var target = new SqlStringParser("$");
        var result = target.parseString(sp);
        assertEquals(12, result.size());
        var i = -1;
        assertEquals("create or replace procedure transfer(\n" +
                "   sender int,", result.get(++i));
        assertEquals("receiver int,", result.get(++i));
        assertEquals("amount dec\n" +
                ")\n" +
                "language plpgsql    \n" +
                "as", result.get(++i));
        assertEquals("$$", result.get(++i));
        assertEquals("begin", result.get(++i));
        assertEquals("update accounts", result.get(++i));
        assertEquals("'test'", result.get(++i));
        assertEquals("set balance = balance - amount \n" +
                "    where id = sender;", result.get(++i));

        assertEquals("update accounts \n" +
                "    set balance = balance + amount \n" +
                "    where id = receiver;", result.get(++i));

        assertEquals("commit;", result.get(++i));
        assertEquals("end;", result.get(++i));

        assertEquals("$$", result.get(++i));

    }
}
