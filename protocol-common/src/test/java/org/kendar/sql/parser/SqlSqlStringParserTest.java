package org.kendar.sql.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlSqlStringParserTest {

    @Test
    void prepStatementForMysql(){
        var  query= "INSERT INTO `task` (`archive_date`, `notes`, `priority`, `status`, `task_name`)\n" +
                "\n" +
                "VALUES (@NULL, 'vvv', 'High', @NULL, 'aa');\n" +
                "\n" +
                "SELECT `id`\n" +
                "\n" +
                "FROM `task`\n" +
                "\n" +
                "WHERE ROW_COUNT() = 1 AND `id` = LAST_INSERT_ID()";
        var target = new SqlStringParser("$");
        var result = target.getTypes(query);
        assertEquals(2, result.size());

    }
    @Test
    void parseCalls() {
        var target = new SqlStringParser("$");
        var result = target.parseString("select * from hello($1, $2 )  as result");
        assertEquals(6, result.size());
        assertEquals(",", result.get(2));
    }

    @Test
    void somethingWeird() {
        var target = new SqlStringParser("$");
        var result = target.parseString("insert into COMPANY_JPA (ADDRESS, AGE, DENOMINATION, SALARY) values ($1, $2, $3, $4)\nRETURNING *");
        assertEquals(15, result.size());
    }


    @Test
    public void spParametersEnd() {
        var target = new SqlStringParser("$");
        var result = target.parseString("Example 'te''st' simple=$1");
        assertEquals(4, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("'te''st'", result.get(1));
        assertEquals(" simple=", result.get(2));
        assertEquals("$1", result.get(3));
    }

    @Test
    public void spParametersMiddle() {
        var target = new SqlStringParser("$");
        var result = target.parseString("Example 'te''st'a=$k simple");
        assertEquals(5, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("'te''st'", result.get(1));
        assertEquals("a=", result.get(2));
        assertEquals("$k", result.get(3));
        assertEquals(" simple", result.get(4));
    }

    @Test
    public void spParametersInString() {
        var target = new SqlStringParser("$");
        var result = target.parseString("Example 'te''st$k'a= simple");
        assertEquals(3, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("'te''st$k'", result.get(1));
        assertEquals("a= simple", result.get(2));
    }

    @Test
    public void quotesHandling() {
        var target = new SqlStringParser("$");
        var result = target.parseString("Example 'te''st' simple");
        assertEquals(3, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("'te''st'", result.get(1));
        assertEquals(" simple", result.get(2));

        result = target.parseString("Example 'test' simple");
        assertEquals(3, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("'test'", result.get(1));
        assertEquals(" simple", result.get(2));

        result = target.parseString("Example 'te\\'st' simple");
        assertEquals(3, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("'te\\'st'", result.get(1));
        assertEquals(" simple", result.get(2));

        result = target.parseString("Example \"test\" simple");
        assertEquals(3, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("\"test\"", result.get(1));
        assertEquals(" simple", result.get(2));

        result = target.parseString("Example \"te\\\"st\" simple");
        assertEquals(3, result.size());
        assertEquals("Example ", result.get(0));
        assertEquals("\"te\\\"st\"", result.get(1));
        assertEquals(" simple", result.get(2));
    }

    @Test
    public void semicolumnHandling() {
        var target = new SqlStringParser("$");
        var result = target.parseSql("SELECT 1;UPDATE test SET name='y''o' WHERE 1=0;");
        assertEquals(2, result.size());
        assertEquals("SELECT 1;", result.get(0));
        assertEquals("UPDATE test SET name='y''o' WHERE 1=0;", result.get(1));

        result = target.parseSql("SELECT 1;UPDATE test SET name='y''o' WHERE 1=0");
        assertEquals(2, result.size());
        assertEquals("SELECT 1;", result.get(0));
        assertEquals("UPDATE test SET name='y''o' WHERE 1=0", result.get(1));
    }

    @Test
    public void standardQuery() {
        var target = new SqlStringParser("$");
        var result = target.getTypes("SELECT 1;UPDATE test SET name='y''o' WHERE 1=0;");
        assertEquals(2, result.size());
        assertEquals("SELECT 1;", result.get(0).getValue());
        assertEquals(SqlStringType.SELECT, result.get(0).getType());
        assertEquals("UPDATE test SET name='y''o' WHERE 1=0;", result.get(1).getValue());
        assertEquals(SqlStringType.UPDATE, result.get(1).getType());
    }

    @Test
    public void withError() {
        var target = new SqlStringParser("$");
        var result = target.getTypes("\r\nDROP TABLE IF EXISTS temp_table1 CASCADE;\r\n CREATE TABLE temp_table1 (intf int);\r\n");
        assertEquals(2, result.size());
        assertEquals("\r\nDROP TABLE IF EXISTS temp_table1 CASCADE;", result.get(0).getValue());
        assertEquals(SqlStringType.UPDATE, result.get(0).getType());
        assertEquals("\r\n CREATE TABLE temp_table1 (intf int);", result.get(1).getValue());
        assertEquals(SqlStringType.UPDATE, result.get(1).getType());
    }

    @Test
    void somethingWeird2() {
        var target = new SqlStringParser("$");
        var result = target.getTypes("insert into COMPANY_JPA (ADDRESS, AGE, DENOMINATION, SALARY) values ($1, $2, $3, $4)\nRETURNING *");
        assertEquals(1, result.size());
    }
}
