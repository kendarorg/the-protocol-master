package org.kendar.utils;

import org.junit.jupiter.api.Test;
import org.kendar.utils.parser.NumberClass;
import org.kendar.utils.parser.SimpleClass;
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.SubObject;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleParserTest {
    @Test
    void simple() {
        var target = new SimpleParser();
        var result = target.parse("abc = '\\'test'");
        assertEquals("Token{type=BLOCK, children=[" +
                "Token{type=VARIABLE, value='abc'}, " +
                "Token{type=OPERATOR, value='='}, " +
                "Token{type=STRING, value='\\'test'}" +
                "]}",result.toString());
    }

    @Test
    void simpleNumbers() {
        var target = new SimpleParser();
        var result = target.parse("abc = 22");
        assertEquals("Token{type=BLOCK, children=[" +
                "Token{type=VARIABLE, value='abc'}, " +
                "Token{type=OPERATOR, value='='}, " +
                "Token{type=NUMBER, value='22'}" +
                "]}",result.toString());
    }

    @Test
    void simpleNumbersComma() {
        var target = new SimpleParser();
        var result = target.parse("abc = 22.77");
        assertEquals("Token{type=BLOCK, children=[" +
                "Token{type=VARIABLE, value='abc'}, " +
                "Token{type=OPERATOR, value='='}, " +
                "Token{type=NUMBER, value='22.77'}" +
                "]}",result.toString());
    }



    @Test
    void simpleNumbersCommaZero() {
        var target = new SimpleParser();
        var result = target.parse("abc = 0.77");
        assertEquals("Token{type=BLOCK, children=[" +
                "Token{type=VARIABLE, value='abc'}, " +
                "Token{type=OPERATOR, value='='}, " +
                "Token{type=NUMBER, value='0.77'}" +
                "]}",result.toString());
    }

    @Test
    void nested() {
        var target = new SimpleParser();
        var result = target.parse("a = A OR ( b+c 'd' e (f=d))");
        assertEquals("Token{type=BLOCK, children=[" +
                        "Token{type=VARIABLE, value='a'}, " +
                        "Token{type=OPERATOR, value='='}, " +
                        "Token{type=VARIABLE, value='A'}, " +
                        "Token{type=FUNCTION, value='OR', children=[" +
                        "Token{type=BLOCK, children=[" +
                        "Token{type=VARIABLE, value='b'}, " +
                        "Token{type=OPERATOR, value='+'}, " +
                        "Token{type=VARIABLE, value='c'}, " +
                        "Token{type=STRING, value='d'}, " +
                        "Token{type=VARIABLE, value='e'}, " +
                        "Token{type=BLOCK, children=[" +
                        "Token{type=VARIABLE, value='f'}, " +
                        "Token{type=OPERATOR, value='='}, " +
                        "Token{type=VARIABLE, value='d'}]}]}]}]}",
                result.toString());
    }

    @Test
    void missingCloseBracket() {
        var target = new SimpleParser();
        assertThrows(RuntimeException.class,()->target.parse("a = A OR ( b+c 'd' e (f=d) c"),"Missing closing bracket");
    }

    @Test
    void missingOpenBracket() {
        var target = new SimpleParser();
        assertThrows(RuntimeException.class,()->target.parse("a = A OR ) c"),"Missing closing bracket");
    }

    @Test
    void missingString() {
        var target = new SimpleParser();
        assertThrows(RuntimeException.class,()->target.parse("a = A OR ( b+c 'd e (f=d) c"),"Wrong string separator");
    }

    @Test
    void realistic() {
        var target = new SimpleParser();
        var result = target.parse("OR(a='test',AND(b=c,d='e',CONCAT('f','g',h)),TRUE)");
        assertEquals("Token{type=BLOCK, children=[" +
                        "Token{type=FUNCTION, value='OR', children=[" +
                        "Token{type=BLOCK, children=[" +
                        "Token{type=VARIABLE, value='a'}, " +
                        "Token{type=OPERATOR, value='='}, " +
                        "Token{type=STRING, value='test'}]}, " +
                        "Token{type=FUNCTION, value='AND', children=[" +
                        "Token{type=BLOCK, children=[" +
                        "Token{type=VARIABLE, value='b'}, " +
                        "Token{type=OPERATOR, value='='}, " +
                        "Token{type=VARIABLE, value='c'}]}, " +
                        "Token{type=BLOCK, children=[" +
                        "Token{type=VARIABLE, value='d'}, " +
                        "Token{type=OPERATOR, value='='}, " +
                        "Token{type=STRING, value='e'}]}, " +
                        "Token{type=FUNCTION, value='CONCAT', children=[" +
                        "Token{type=STRING, value='f'}, " +
                        "Token{type=STRING, value='g'}, " +
                        "Token{type=VARIABLE, value='h'}]}]}, " +
                        "Token{type=FUNCTION, value='TRUE'}]}]}",
                result.toString());
    }



//    @Test
//    void evaluateComplex() {
//        var target = new SimpleParser();
//        var result = target.parse("OR(a=='test',AND(b==c,d=='e',CONCAT('f','g',h)),TRUE)");
//        var testClass = new SimpleClass("test", "b","b", "e", "h");
//        var jsonMapper = new JsonMapper();
//        target.evaluate(result,jsonMapper.toJsonNode(testClass));
//    }

    @Test
    void evaluate() {
        var target = new SimpleParser();
        var toExecutOk = target.parse("a=='test'");
        var toExecutFalse = target.parse("a=='toast'");
        var testClass = new SimpleClass("test", "b","b", "e", "h");
        var jsonMapper = new JsonMapper();

        assertTrue((Boolean)target.evaluate(toExecutOk,jsonMapper.toJsonNode(testClass)));
        assertFalse((Boolean)target.evaluate(toExecutFalse,jsonMapper.toJsonNode(testClass)));
    }

    @Test
    void evaluateChildObject() {
        var target = new SimpleParser();
        var toExecutOk = target.parse("child.a=='test'");
        var toExecutFalse = target.parse("child.a=='toast'");
        var testClass = new SubObject("name",new SimpleClass("test", "b","b", "e", "h"));
        var jsonMapper = new JsonMapper();

        assertTrue((Boolean)target.evaluate(toExecutOk,jsonMapper.toJsonNode(testClass)));
        assertFalse((Boolean)target.evaluate(toExecutFalse,jsonMapper.toJsonNode(testClass)));
    }


    @Test
    void workOnFunctionAndOr() {
        var target = new SimpleParser();
        var toExecutOk = target.parse("OR(child.a=='test',name=='thenadasdme')");
        var toExecutFalse = target.parse("AND(child.a=='test',name=='thasename')");
        var testClass = new SubObject("thename",new SimpleClass("test", "b","b", "e", "h"));
        var jsonMapper = new JsonMapper();

        assertTrue((Boolean)target.evaluate(toExecutOk,jsonMapper.toJsonNode(testClass)));
        assertFalse((Boolean)target.evaluate(toExecutFalse,jsonMapper.toJsonNode(testClass)));
    }

    @Test
    void workOnFunctionConcat() {
        var target = new SimpleParser();
        var toExecutOk = target.parse("AND(child.a=='test',name==CONCAT('the','name'))");
        var toExecutFalse = target.parse("AND(child.a=='test',name=='thasename')");
        var testClass = new SubObject("thename",new SimpleClass("test", "b","b", "e", "h"));
        var jsonMapper = new JsonMapper();

        assertTrue((Boolean)target.evaluate(toExecutOk,jsonMapper.toJsonNode(testClass)));
        assertFalse((Boolean)target.evaluate(toExecutFalse,jsonMapper.toJsonNode(testClass)));
    }

    @Test
    void workOnLike() {
        var target = new SimpleParser();
        var toExecutOk = target.parse("AND(child.a=='test',LIKE(name,'the'))");
        var toExecutFalse = target.parse("AND(child.a=='test',name=='thasename')");
        var testClass = new SubObject("thename",new SimpleClass("test", "b","b", "e", "h"));
        var jsonMapper = new JsonMapper();

        assertTrue((Boolean)target.evaluate(toExecutOk,jsonMapper.toJsonNode(testClass)));
        assertFalse((Boolean)target.evaluate(toExecutFalse,jsonMapper.toJsonNode(testClass)));
    }

    @Test
    void workOnNumber() {
        var target = new SimpleParser();
        var toExecutOk1 = target.parse("AND(intValue==(12-11))");
        var toExecutOk2 = target.parse("AND(doubleValue==(1.2+1))");
        var toExecutOk3 = target.parse("AND(doubleValue==(1.2+1),booleanValue)");
        var toExecutOk4 = target.parse("AND(doubleValue==(1.2+1),booleanValue!=FALSE)");
        var toExecutFalse = target.parse("AND(child.a=='test',name=='thasename')");
        var testClass = new NumberClass(1,2.2,3.3f,new BigDecimal("4.4"),true);
        var jsonMapper = new JsonMapper();


        assertTrue((Boolean)target.evaluate(toExecutOk4,jsonMapper.toJsonNode(testClass)));
        assertTrue((Boolean)target.evaluate(toExecutOk3,jsonMapper.toJsonNode(testClass)));
        assertTrue((Boolean)target.evaluate(toExecutOk2,jsonMapper.toJsonNode(testClass)));
        assertTrue((Boolean)target.evaluate(toExecutOk1,jsonMapper.toJsonNode(testClass)));
        assertFalse((Boolean)target.evaluate(toExecutFalse,jsonMapper.toJsonNode(testClass)));
    }
}
