package org.kendar.sql.parser;

import org.kendar.sql.parser.dtos.SimpleToken;
import org.kendar.sql.parser.dtos.TokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private static ArrayList<String> extractMatchingBlocks(String test, Pattern p) {

        var m = p.matcher(test);
        var result = new ArrayList<String>();
        var prevStart = 0;
        if (m.find()) {
            do {
                var start = m.start(0);
                var end = m.end(0);
                if (start > 0) {
                    if (start - prevStart > 0) {
                        result.add(test.substring(prevStart, start));
                    }
                }
                if (end - start > 0) {
                    result.add(test.substring(start, end));
                }
                prevStart = end;
            } while (m.find(prevStart));
        }
        var full = String.join("", result);
        if (full.length() != test.length()) {
            result.add(test.substring(prevStart));
        }
        return result;
    }

    public String getParameterSeparator() {
        return parameterSeparator;
    }

    public boolean isUnknown(List<SqlParseResult> data) {
        return data.stream().anyMatch(a -> a.getType() == SqlStringType.UNKNOWN || a.getType() == SqlStringType.NONE);
    }

    private SqlStringType inferType(String sql) {
        var split = sql.trim().split("\\s+");
        if (split.length != 0) {
            var first = split[0].trim().toLowerCase(Locale.ROOT);
            if (!first.isEmpty()) {
                if (select.containsKey(first)) {
                    return select.get(first);
                }
                return SqlStringType.UNKNOWN;
            }
        }
        return null;
    }

    public List<SqlParseResult> getTypes(String input) {
        var result = new ArrayList<SqlParseResult>();
        try {
            var sqls = parseSql(input);
            for (var sql : sqls) {
                var foundedType = inferType(sql);
                if (foundedType == null || foundedType == SqlStringType.UNKNOWN) {
                    foundedType = inferType(String.join(" ", parseString(sql)));
                }
                if (foundedType != null) {
                    result.add(new SqlParseResult(sql, foundedType));
                }
            }
        } catch (Exception ex) {
            log.error("Unable to split query: {}", input);
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
        var split = parseString(input);


        String tempValue = "";
        var isProcedure = false;
        for (var line : split) {
            var trimmed = line.strip();
            if (trimmed.startsWith("'") || trimmed.startsWith("\"")) {
                //Is a string
                tempValue += line + " ";
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
        return parseStringSimpleTokens(input).stream().
                filter(a -> a.getType() != TokenType.COMMENT && !a.getValue().trim().isEmpty()).
                map(SimpleToken::getValue).collect(Collectors.toList());
    }

    public List<SimpleToken> parseStringSimpleTokens(String input) {
        List<SimpleToken> tokens = new ArrayList<>();
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
                tokens.add(new SimpleToken(TokenType.VALUE_ITEM, sb.toString()));
            } else {
                // Handle non-string token
                StringBuilder sb = new StringBuilder();
                while (i < length) {
                    c = input.charAt(i);
                    if (c == '-' && (i + 1) < length && input.charAt(i + 1) == '-') {
                        tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
                        sb = new StringBuilder();
                        //noinspection ConstantValue It's a loop in loop
                        while ((i < length) && (c != '\n' && c != '\r' && c != '\f')) {
                            sb.append(c);
                            i++;
                            c = input.charAt(i);
                        }
                        tokens.add(new SimpleToken(TokenType.COMMENT, sb.toString()));
                        sb = new StringBuilder();
                        continue;
                    }
                    if (c == ',') {
                        sb.append(c);
                        tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
                        sb = new StringBuilder();
                        i++;
                        continue;
                    }

                    if (c == ';') {
                        sb.append(c);
                        tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
                        sb = new StringBuilder();
                        i++;
                        continue;
                    }

                    if (c == '/' && (i + 1) < length && input.charAt(i + 1) == '*') {
                        tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
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
                        tokens.add(new SimpleToken(TokenType.COMMENT, sb.toString()));
                        sb = new StringBuilder();
                        continue;
                    }
                    if (c == '#') {
                        tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
                        sb = new StringBuilder();
                        //noinspection ConstantValue It's a loop in loop
                        while ((i < length) && (c != '\n' && c != '\r' && c != '\f')) {
                            sb.append(c);
                            i++;
                            c = input.charAt(i);
                        }
                        tokens.add(new SimpleToken(TokenType.COMMENT, sb.toString()));
                        sb = new StringBuilder();
                        continue;
                    }
                    if (parameterSeparator.equalsIgnoreCase(String.valueOf(c))) {
                        tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
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
                                tokens.add(new SimpleToken(TokenType.QUERY_PARAM, sb.toString()));
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
                tokens.add(new SimpleToken(TokenType.BLOB, sb.toString()));
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

    public List<SimpleToken> tokenize(String input) {
        var partial = parseStringSimpleTokens(input).stream().
                filter(a -> !a.getValue().isEmpty()).toList();
        var result = new ArrayList<SimpleToken>();
        for (var token : partial) {
            if (token.getType() == TokenType.BLOB) {
                result.addAll(parseBlob(token));
            } else {
                result.add(token);
            }
        }
        return result;
    }

    private List<SimpleToken> parseBlob(SimpleToken token) {
        var ls = new ArrayList<SimpleToken>();

        var test = token.getValue();

        var p = Pattern.compile("`[a-zA-Z0-9_\\-\\.]+`");
        var result = extractMatchingBlocks(test, p);
        for (var item : result) {
            if (item.startsWith("`") && item.endsWith("`")) {
                ls.add(new SimpleToken(TokenType.SINGLE_ITEM, item));
            } else {
                for (var subItem : buildNumbers(item, Pattern.compile("([+-]*[0-9]+\\.[0-9]+)"))) {
                    if (subItem.getType() != TokenType.BLOB) {
                        ls.add(subItem);
                    } else {
                        var px = Pattern.compile("([+-]*[0-9]+)");
                        for (var sub : buildNumbers(subItem.getValue(), px)) {
                            if (sub.getType() != TokenType.BLOB) {
                                ls.add(sub);
                            } else {
                                if (sub.getValue().contains(",")) {
                                    ls.addAll(handleComma(sub));
                                } else {
                                    ls.add(sub);
                                }
                            }
                        }
                    }
                }

            }
        }
        for (var item : ls) {
            if (item.getValue().equalsIgnoreCase("NULL") || item.getValue().equalsIgnoreCase("@NULL")) {
                item.setType(TokenType.VALUE_ITEM);
            }
        }
        return ls;
    }

    private List<SimpleToken> handleComma(SimpleToken sub) {
        var test = sub.getValue();
        var result = new ArrayList<SimpleToken>();
        var prev = "";
        for (var c : test.toCharArray()) {
            if (c == ',') {
                if (!prev.trim().isEmpty()) {
                    result.add(new SimpleToken(TokenType.BLOB, prev.trim()));
                }
                result.add(new SimpleToken(TokenType.SINGLE_ITEM, ","));
                prev = "";
            } else {
                prev += c;
            }
        }
        if (!prev.isEmpty()) {
            result.add(new SimpleToken(TokenType.BLOB, prev.trim()));
        }
        return result;
    }

    private List<SimpleToken> buildNumbers(String startItem, Pattern p) {
        var ls = new ArrayList<SimpleToken>();
        String[] items = startItem.trim().split("[\n\r\f\\s]+");
        for (var subItem : items) {
            subItem = subItem.trim();
            var result = extractMatchingBlocks(subItem, p);
            for (var item : result) {
                if (p.matcher(item).matches()) {
                    ls.add(new SimpleToken(TokenType.VALUE_ITEM, item));
                } else {
                    ls.add(new SimpleToken(TokenType.BLOB, item));

                }
            }
        }
        return ls;
    }
}
