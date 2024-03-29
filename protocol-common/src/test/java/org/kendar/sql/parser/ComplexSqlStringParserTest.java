package org.kendar.sql.parser;

import org.junit.jupiter.api.Test;

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
        assertEquals(14, result.size());
        var i = -1;
        assertEquals("create or replace procedure transfer(\n" +
                "   sender int,", result.get(++i));
        assertEquals("\n" +
                "   receiver int,", result.get(++i));
        assertEquals(" \n" +
                "   amount dec\n" +
                ")\n" +
                "language plpgsql    \n" +
                "as ", result.get(++i));
        assertEquals("$$", result.get(++i));
        assertEquals("\n" +
                "begin\n" +
                "    ", result.get(++i));
        assertEquals("\n" +
                "    update accounts ", result.get(++i));
        assertEquals("'test'", result.get(++i));
        assertEquals(" \n" +
                "    set balance = balance - amount \n" +
                "    where id = sender;", result.get(++i));
        i++;
        assertEquals("\n" +
                "    update accounts \n" +
                "    set balance = balance + amount \n" +
                "    where id = receiver;", result.get(++i));

        assertEquals("\n" +
                "\n" +
                "    commit;", result.get(++i));
        assertEquals("\n" +
                "end;", result.get(++i));
        i++;
        assertEquals("$$", result.get(++i));

    }
}
