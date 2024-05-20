package org.kendar.sql.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("StringConcatenationInLoop")
public class SqlStringParser {

    private static final Logger log = LoggerFactory.getLogger(SqlStringParser.class);
    final Map<String, SqlStringType> select;
    private final String parameterSeparator;

    {
        select = new ConcurrentHashMap<>();
        select.put("select", SqlStringType.SELECT);
        select.put("show", SqlStringType.SELECT);
        select.put("explain", SqlStringType.SELECT);
        select.put("describe", SqlStringType.SELECT);
        select.put("fetch", SqlStringType.SELECT);

        select.put("update", SqlStringType.UPDATE);
        select.put("insert", SqlStringType.INSERT);
        select.put("create", SqlStringType.UPDATE);
        select.put("delete", SqlStringType.UPDATE);
        select.put("merge", SqlStringType.UPDATE);
        select.put("alter", SqlStringType.UPDATE);
        select.put("drop", SqlStringType.UPDATE);
        select.put("grant", SqlStringType.UPDATE);
        select.put("set", SqlStringType.UPDATE);
        select.put("truncate", SqlStringType.UPDATE);
        select.put("declare", SqlStringType.UPDATE);

        select.put("call", SqlStringType.CALL);
        select.put("execute", SqlStringType.CALL);
        select.put("run", SqlStringType.CALL);
    }

    public SqlStringParser(String parameterSeparator) {
        this.parameterSeparator = parameterSeparator;
    }

    public String getParameterSeparator() {
        return parameterSeparator;
    }

    public boolean isUnknown(List<SqlParseResult> data) {
        return data.stream().anyMatch(a -> a.getType() == SqlStringType.UNKNOWN || a.getType() == SqlStringType.NONE);
    }

    public List<SqlParseResult> getTypes(String input) {
        var result = new ArrayList<SqlParseResult>();
        try {
            var sqls = parseSql(input);
            for (var sql : sqls) {
                var splitted = sql.trim().split("\\s+");
                if (splitted.length == 0) {
                    continue;
                }
                var first = splitted[0].trim().toLowerCase(Locale.ROOT);
                if (first.isEmpty()) {
                    continue;
                }
                if (select.containsKey(first)) {
                    var founded = select.get(first);
                    result.add(new SqlParseResult(sql, founded));
                } else {
                    result.add(new SqlParseResult(sql, SqlStringType.UNKNOWN));
                }
            }
        } catch (Exception ex) {
            log.error("Unable to split query: " + input);
            result.clear();
            if (input.endsWith(";")) {
                input = input.substring(0, input.length() - 1);
            }
            result.add(new SqlParseResult(input, SqlStringType.UNKNOWN));
        }
        return result;
    }

    public List<String> parseSql(String input) {
        List<String> sqls = new ArrayList<>();
        var splitted = parseString(input);


        String tempValue = "";
        var isProcedure = false;
        for (var line : splitted) {
            var trimmed = line.strip();
            if (trimmed.startsWith("'") || trimmed.startsWith("\"")) {
                //Is a string
                tempValue += line;
                continue;
            }
            if (line.toLowerCase().contains("function") || line.toLowerCase().contains("procedure")) {
                isProcedure = true;
            }
            if (!isProcedure && trimmed.contains(";")) {
                for (var part : line.split("((?<=;))")) {
                    var trimPart = part.trim();
                    if (trimPart.endsWith(";")) {
                        tempValue += part;
                        sqls.add(tempValue);
                        tempValue = "";
                    } else {
                        tempValue += part;
                    }
                }
                continue;
            }
            tempValue += line;
        }
        if (!tempValue.isEmpty()) {
            sqls.add(tempValue);
        }
        return sqls;
    }

    public List<String> parseString(String input) {
        List<String> tokens = new ArrayList<>();
        int length = input.length();
        int i = 0;
        while (i < length) {
            char c = input.charAt(i);
            if (c == '\'' || c == '\"') {
                StringBuilder sb = new StringBuilder();
                char delimiter = c;
                sb.append(c); // include starting quote
                i++; // skip delimiter
                while (i < length) {
                    c = input.charAt(i);
                    if (c == delimiter) {
                        if (i + 1 < length && input.charAt(i + 1) == delimiter && delimiter == '\'') {
                            // Handle doubled delimiter
                            sb.append(delimiter);
                            sb.append(delimiter);
                            i += 2;
                        } else {
                            // End of string
                            sb.append(c);
                            i++;
                            break;
                        }
                    } else if (c == '\\') {
                        // Handle escaped character
                        sb.append(c);
                        i++;
                        if (i < length) {
                            sb.append(input.charAt(i));
                            i++;
                        }
                    } else {
                        sb.append(c);
                        i++;
                    }
                }
                tokens.add(sb.toString());
            } else {
                // Handle non-string token
                StringBuilder sb = new StringBuilder();
                while (i < length) {
                    c = input.charAt(i);
                    if (c == '-' && (i + 1) < length && input.charAt(i + 1) == '-') {
                        tokens.add(sb.toString());
                        sb = new StringBuilder();
                        while ((i < length) && (c != '\n' && c != '\r' && c != '\f')) {
                            sb.append(c);
                            i++;
                            c = input.charAt(i);
                        }
                        sb = new StringBuilder();
                        continue;
                    }
                    if (c == ',') {
                        sb.append(c);
                        tokens.add(sb.toString());
                        sb = new StringBuilder();
                        i++;
                        continue;
                    }

                    if (c == ';') {
                        sb.append(c);
                        tokens.add(sb.toString());
                        sb = new StringBuilder();
                        i++;
                        continue;
                    }

                    if (c == '/' && (i + 1) < length && input.charAt(i + 1) == '*') {
                        tokens.add(sb.toString());
                        sb = new StringBuilder();
                        while (i < length - 1) {
                            if (c == '*' && input.charAt(i + 1) == '/') {
                                sb.append("*/");
                                i += 2;
                                break;
                            }
                            sb.append(c);
                            i++;
                            c = input.charAt(i);
                        }
                        //tokens.add(sb.toString());
                        sb = new StringBuilder();
                        continue;
                    }
                    if (c == '#') {
                        tokens.add(sb.toString());
                        sb = new StringBuilder();
                        //Wrong suggestion!
                        while ((i < length) && (c != '\n' && c != '\r' && c != '\f')) {
                            sb.append(c);
                            i++;
                            c = input.charAt(i);
                        }
                        sb = new StringBuilder();
                        continue;
                    }
                    if (parameterSeparator.equalsIgnoreCase(String.valueOf(c))) {
                        tokens.add(sb.toString());
                        sb = new StringBuilder();
                        sb.append(c);
                        i++;
                        while (i < length) {
                            c = input.charAt(i);
                            if (c == '$' || (c > 'a' && c < 'z') || (c > 'A' && c < 'Z')
                                    || (c > '0' && c < '9') || c == '_') {
                                sb.append(c);
                                i++;
                            } else {
                                tokens.add(sb.toString());
                                sb = new StringBuilder();
                                break;
                            }
                        }
                        continue;

                    }
                    if (c == '\'' || c == '\"') {
                        break;
                    } else {
                        sb.append(c);
                        i++;
                    }
                }
                tokens.add(sb.toString());
            }
        }
        return tokens;
    }


    public boolean isMixed(List<SqlParseResult> parsed) {
        SqlStringType founded = SqlStringType.NONE;
        for (var single : parsed) {
            if (founded == SqlStringType.NONE) {
                founded = single.getType();
            }
            if (single.getType() != founded) {
                if (single.getType() == SqlStringType.INSERT && founded == SqlStringType.UPDATE) {
                    continue;
                } else if (single.getType() == SqlStringType.UPDATE && founded == SqlStringType.INSERT) {
                    continue;
                }
                return true;
            }

        }
        return false;
    }
}
