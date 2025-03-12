package org.kendar.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.kendar.utils.parser.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SortGroupParserTest {
    private static final JsonMapper mapper = new JsonMapper();

    @Test
    void simple() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(WHERE(val == 'a'))");
        assertEquals("Token{type=BLOCK, children=[Token{type=FUNCTION, value='SELECT', children=[Token{type=FUNCTION, value='WHERE', children=[Token{type=BLOCK, children=[Token{type=VARIABLE, value='val'}, Token{type=OPERATOR, value='=='}, Token{type=STRING, value='a'}]}]}]}]}", parsed.toString());
        var data = buildTestItems();
        var result = target.select(parsed, (ArrayNode) data);
        assertEquals(1, result.size());
    }

    private JsonNode buildTestItems() {
        return new JsonMapper().toJsonNode(List.of(new ItemWithValAndCount("a", 1), new ItemWithValAndCount("b", 2), new ItemWithValAndCount("c", 3)));
    }

    @Test
    void simpleWhat() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(WHAT(result=val))");
        var data = buildTestItems();
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<SimpleResult>>() {
        });

        assertEquals(3, result.size());
    }

    @Test
    void simpleWhatWithFunction() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(WHAT(result=COUNT(val)))");
        var data = buildTestItems();
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<SimpleNumericResult>>() {
        });

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(a -> a.getResult() == 1));
    }


    @Test
    void simpleGroup() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(GROUPBY(val))");
        var data = new JsonMapper().toJsonNode(List.of(
                new ItemWithValAndCount("a", 1),
                new ItemWithValAndCount("b", 2),
                new ItemWithValAndCount("c", 3),
                new ItemWithValAndCount("c", 4),
                new ItemWithValAndCount("a", 6)));
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<SimpleValResult>>() {
        });

        assertEquals(3, result.size());
    }

    @Test
    void groupWithAssignement() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(WHAT(counter=COUNT(),val=val),GROUPBY(val))");
        var data = new JsonMapper().toJsonNode(List.of(
                new ItemWithValAndCount("a", 1),
                new ItemWithValAndCount("b", 2),
                new ItemWithValAndCount("c", 3),
                new ItemWithValAndCount("c", 4),
                new ItemWithValAndCount("a", 6)));
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<ItemWithValAndCount>>() {
        });

        assertEquals(3, result.size());
    }

    @Test
    void groupWithSUM() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(WHAT(counter=SUM(counter),val=val),GROUPBY(val))");
        var data = new JsonMapper().toJsonNode(List.of(
                new ItemWithValAndCount("a", 1),
                new ItemWithValAndCount("b", 2),
                new ItemWithValAndCount("c", 3),
                new ItemWithValAndCount("c", 4),
                new ItemWithValAndCount("a", 6)));
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<ItemWithValAndCount>>() {
        });

        assertEquals(3, result.size());
    }

    @Test
    void groupWithOB() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(ORDERBY(ASC(counter)))");
        var data = new JsonMapper().toJsonNode(List.of(
                new ItemWithValAndCount("a", 6),
                new ItemWithValAndCount("b", 2),
                new ItemWithValAndCount("c", 3),
                new ItemWithValAndCount("c", 4),
                new ItemWithValAndCount("a", 1)));
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<ItemWithValAndCount>>() {
        });

        assertEquals(5, result.size());
        assertEquals(1,result.get(0).getCounter());
        assertEquals(6,result.get(4).getCounter());
    }

    @Test
    void groupWithDouble() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(ORDERBY(ASC(val),DESC(counter)))");
        var data = new JsonMapper().toJsonNode(List.of(
                new ItemWithValAndCount("a", 6),
                new ItemWithValAndCount("b", 2),
                new ItemWithValAndCount("c", 3),
                new ItemWithValAndCount("c", 4),
                new ItemWithValAndCount("a", 1)));
        var result = mapper.deserialize(target.select(parsed, (ArrayNode) data), new TypeReference<List<ItemWithValAndCount>>() {
        });

        assertEquals(5, result.size());
        assertEquals(6,result.get(0).getCounter());
        assertEquals("a",result.get(0).getVal());
        assertEquals(1,result.get(1).getCounter());
        assertEquals("a",result.get(1).getVal());
        assertEquals(2,result.get(2).getCounter());
        assertEquals("b",result.get(2).getVal());
        assertEquals(4,result.get(3).getCounter());
        assertEquals("c",result.get(3).getVal());
        assertEquals(3,result.get(4).getCounter());
        assertEquals("c",result.get(4).getVal());
    }

    @Test
    void testComplex() {
        var target = new SimpleParser();
        var parsed = target.parse("SELECT(WHAT(date=MSTODATE(timestamp),instanceId,protocol,\n" +
                "                            query=SUBSTR(query,20),duration),ORDERBY(ASC(date)))");
        assertEquals(
    "Token{type=FUNCTION, value='SELECT', children=[Token{type=FUNCTION, value='WHAT', children=[Token{type=BLOCK, children=[Token{type=VARIABLE, value='date'}, Token{type=OPERATOR, value='='}, Token{type=FUNCTION, value='MSTODATE', children=[Token{type=BLOCK, children=[Token{type=VARIABLE, value='timestamp'}]}]}]}, Token{type=VARIABLE, value='instanceId'}, Token{type=VARIABLE, value='protocol'}, Token{type=BLOCK, children=[Token{type=VARIABLE, value='query'}, Token{type=OPERATOR, value='='}, Token{type=FUNCTION, value='SUBSTR', children=[Token{type=BLOCK, children=[Token{type=VARIABLE, value='query'}]}, Token{type=BLOCK, children=[Token{type=NUMBER, value='20'}]}]}]}, Token{type=VARIABLE, value='duration'}]}, Token{type=BLOCK, children=[Token{type=VARIABLE, value='ORDERBY'}, Token{type=BLOCK, children=[Token{type=VARIABLE, value='ASC'}, Token{type=BLOCK, children=[Token{type=VARIABLE, value='date'}]}]}]}]}",parsed.toString());
    }
}
