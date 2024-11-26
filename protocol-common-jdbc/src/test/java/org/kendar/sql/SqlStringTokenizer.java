package org.kendar.sql;

import org.junit.jupiter.api.Test;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.sql.parser.dtos.TokenType;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class SqlStringTokenizer {
    @Test
    void matcherSetupBackComma(){
        String test = "INSERT INTO `task` (`archive_date`, `notes`,`a``b`";
        var p = Pattern.compile("`[a-zA-Z0-9_\\-\\.]+`");
        var m = p.matcher(test);
        var result = new ArrayList<String>();
        var prevStart = 0;
        if(m.find()) {
            do {
                var start = m.start(0);
                var end = m.end(0);
                if(start>0){
                    if(start-prevStart>0){
                        result.add(test.substring(prevStart, start));
                    }
                }
                if(end-start>0){
                    result.add(test.substring(start, end));
                }
                prevStart = end;

                System.out.println(m.group());
            } while(m.find(prevStart));
        }
        var full = String.join("",result);
        if(full.length()!=test.length()){
            result.add(test.substring(prevStart));
        }

        System.out.println(result);
    }

    @Test
    void matcherSetupNumbers(){
        String test = "INSERT INTO `task` (`archive_date`, `notes`,`a``b`";
        var p = Pattern.compile("([+-]*[0-9]+)|([+-]*[0-9]+\\.[0-9]+)");
        assertTrue(p.matcher("+22").matches());
        assertTrue(p.matcher("-22").matches());
        assertTrue(p.matcher("22").matches());
        assertTrue(p.matcher("22.5").matches());
        assertTrue(p.matcher("-22.5").matches());
        assertTrue(p.matcher("+22.5").matches());
        assertFalse(p.matcher("+22.5.6").matches());
    }

    @Test
    void splitDelimiters(){
        String test = "INSERT,a,b,";
        var result = new ArrayList<String>();
        var prev  ="";
        for(var c:test.toCharArray()){
            if(c==','){
                if(prev.length()>0){
                    result.add(prev);
                }
                result.add(",");
                prev="";
            }else{
                prev+=c;
            }
        }
        if(prev.length()>0){
            result.add(prev);
        }
        System.out.println(result);
    }

    @Test
    void parseStringWithVals() {
        var query = "INSERT INTO `task` (`archive_date`, `notes`, `priority`, `status`, `task_name`,`test`)\n" +
                "\n" +
                "VALUES (22.5,@NULL, 'vvv', 'High', @NULL, 'aa',$1);\n" +
                "-- this is a comment\n" +
                "SELECT `id`\n" +
                "\n" +
                "FROM `task`\n" +
                "#another comment\n" +
                "WHERE ROW_COUNT() = +11 AND `id` = LAST_INSERT_ID()";
        var target = new SqlStringParser("$");
        var result = target.tokenize(query).stream().filter(a->a.getType()== TokenType.VALUE_ITEM).collect(Collectors.toList());
        assertEquals(7, result.size());

    }
}
