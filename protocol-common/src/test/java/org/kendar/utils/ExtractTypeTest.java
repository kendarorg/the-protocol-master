package org.kendar.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ExtractTypeTest {

    public static List<String> getAllMatches(String text, String regex) {
        List<String> matches = new ArrayList<String>();
        var m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while (m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }

    @Test
    void test() {

        List<String> matches = getAllMatches(
                "/a/{#aaa}/bb/{#ccc}/test",
                "\\{#[a-zA-Z_\\-\\.]+\\}");
        System.out.println("");

    }

}
