package org.kendar.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExtraStringReplacerTest {
    @Test
    void testFirst(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination}' AND AGE=@{age};");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='",
                        "${denomination}",
                        "' AND AGE=",
                        "@{age}",
                        ";"),
                result
        );
    }

    @Test
    void testFirstMath(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination}' AND AGE=@{age};");
        var match = ExtraStringReplacer.match(result,"SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='test va;ie' AND AGE=237;");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='",
                        "test va;ie",
                        "' AND AGE=",
                        "237",
                        ";"),
                match
        );
    }

    @Test
    void testSecond(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination}' AND AGE=@{age;");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='",
                        "${denomination}",
                        "' AND AGE=@{age;"),
                result
        );
    }

    @Test
    void testSecondMatch(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination}' AND AGE=@{age;");
        var match = ExtraStringReplacer.match(result,"SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='test va;ie' AND AGE=@{age;");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='",
                        "test va;ie",
                        "' AND AGE=@{age;"),
                match
        );
    }

    @Test
    void testThird(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=@{age};");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=",
                        "@{age}",
                        ";"),
                result
        );
    }

    @Test
    void testThirdMatch(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=@{age};");
        var match = ExtraStringReplacer.match(result,"SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=237;");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=",
                        "237",
                        ";"),
                match
        );
    }

    @Test
    void testFinalMatch(){
        var result = ExtraStringReplacer.parse("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=@{age}");
        var match = ExtraStringReplacer.match(result,"SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=237");
        assertIterableEquals (
                List.of("SELECT DENOMINATION,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination' AND AGE=",
                        "237"),
                match
        );
    }

    @Test
    void testError(){
       assertThrows(RuntimeException.class,()->ExtraStringReplacer.parse("SELECT ${denomination}@{age};"));
    }
}
