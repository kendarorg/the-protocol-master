package org.kendar.redis.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class Resp3Parser {


    static boolean isUpper(String input) {
        for (char c : input.toCharArray()) {
            if (Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

    private static String buildIntegerString(Resp3Input line) throws Resp3ParseException {
        String result = "";
        var end = 0;
        while (line.hasNext() && end != 2) {
            var ch = line.charAtAndIncrement();
            if (ch == '\r' && end == 0) {
                end++;
            } else if (ch == '\n' && end == 1) {
                end++;
            } else if (((ch >= '0' && ch <= '9') || ch == '-' || ch == '+') && end == 0) {
                result += ch;
            } else {
                throw new Resp3ParseException("Invalid integer format");
            }
        }
        if (end != 2) {
            throw new Resp3ParseException("Unterminated end of integer", true);
        }
        return result;
    }

    private RespError parseSimpleError(Resp3Input line) throws Resp3ParseException {
        var content = parseSimpleString(line);

        var spl = content.split("\\s");
        var result = new RespError();
        if (isUpper(spl[0])) {
            result.setType(spl[0]);
            result.setMsg(content.substring(spl[0].length() + 1));
        } else {
            result.setMsg(content);
        }
        return result;
    }

    private String parseSimpleString(Resp3Input line) throws Resp3ParseException {
        String result = "";
        var end = 0;
        while (line.hasNext() && end != 2) {
            var ch = line.charAtAndIncrement();
            if (ch == '\r' && end == 0) {
                end++;
            } else if (ch == '\n' && end == 1) {
                end++;
            } else if (ch != '\r' && ch != '\n' && end == 0) {
                result += ch;
            } else {
                throw new Resp3ParseException("Invalid string format");
            }
        }
        if (end != 2) {
            throw new Resp3ParseException("Unterminated end of string", true);
        }
        return result;
    }

    private int parseInteger(Resp3Input line) throws Resp3ParseException {
        String result = buildIntegerString(line);
        try {
            return Integer.parseInt(result);
        } catch (Exception ex) {
            throw new Resp3ParseException("Invalid integer");
        }
    }

    private String parseBulkString(Resp3Input line) throws Resp3ParseException {
        int length;
        try {
            length = parseInteger(line);
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid bulk string length", ex.isMissingData());
        }
        String content = null;
        if (length == -1) {
            return content;
        }
        if (line.length() < (length + 2)) {
            throw new Resp3ParseException("Unterminated end of bulk string", true);
        }
        content = line.substring(length);
        if (line.charAtAndIncrement() != '\r') {
            throw new Resp3ParseException("Invalid bulk string format");
        }

        if (line.charAtAndIncrement() != '\n') {
            throw new Resp3ParseException("Invalid bulk string format");
        }
        return content;
    }

    private List<Object> parseArray(Resp3Input line) throws Resp3ParseException {
        int length;
        try {
            length = parseInteger(line);
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid bulk string length", ex.isMissingData());
        }
        List<Object> content = null;
        if (length == -1) {
            return content;
        }
        try {
            content = new ArrayList<>();
            for (var i = 0; i < length; i++) {
                var result = parse(line);
                content.add(result);
            }
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid array format", ex.isMissingData());
        }
        return content;
    }

    private void parseNull(Resp3Input line) throws Resp3ParseException {

        if (line.charAtAndIncrement() != '\r') {
            throw new Resp3ParseException("Invalid null format");
        }

        if (line.charAtAndIncrement() != '\n') {
            throw new Resp3ParseException("Invalid null format");
        }
    }

    private boolean parseBool(Resp3Input line) throws Resp3ParseException {
        var letter = line.charAtAndIncrement();
        if (letter != 't' && letter != 'f') {
            throw new Resp3ParseException("Invalid bool format string");
        }
        if (line.charAtAndIncrement() != '\r') {
            throw new Resp3ParseException("Invalid bool format");
        }

        if (line.charAtAndIncrement() != '\n') {
            throw new Resp3ParseException("Invalid bool format");
        }
        return letter == 't';
    }

    private BigInteger parseBigNumber(Resp3Input line) throws Resp3ParseException {
        String result = buildIntegerString(line);
        try {
            return new BigInteger(result);
        } catch (Exception ex) {
            throw new Resp3ParseException("Invalid integer");
        }
    }

    private double parseDouble(Resp3Input line) throws Resp3ParseException {

        String result = "";
        var end = 0;
        while (line.hasNext() && end != 2) {
            var ch = line.charAtAndIncrement();
            if (ch == '\r' && end == 0) {
                end++;
            } else if (ch == '\n' && end == 1) {
                end++;
            } else if ((ch == '-' || ch == '+' || ch == '.' || ch == 'e'
                    || ch == 'i' || ch == 'n' || ch == 'f' || ch == 'a'
                    || ch == 'E' || (ch >= '0' && ch <= '9')) && end == 0) {
                result += ch;
            } else {
                throw new Resp3ParseException("Invalid integer format");
            }
        }
        if (end != 2) {
            throw new Resp3ParseException("Unterminated end of integer", true);
        }
        if (result.equalsIgnoreCase("inf")) {
            return Double.POSITIVE_INFINITY;
        } else if (result.equalsIgnoreCase("-inf")) {
            return Double.NEGATIVE_INFINITY;
        } else if (result.equalsIgnoreCase("nan")) {
            return Float.NaN;
        }
        var pattern = Pattern.compile("([+\\-]?[0-9\\.]+)([Ee])?([\\-+]?[0-9]*)");
        var matcher = pattern.matcher(result);
        if (!matcher.find()) {
            throw new Resp3ParseException("Invalid double format");
        }
        return Double.parseDouble(result);
    }

    private RespError parseBulkError(Resp3Input line) throws Resp3ParseException {
        var content = parseBulkString(line);

        var spl = content.split("\\s");
        var result = new RespError();
        if (isUpper(spl[0])) {
            result.setType(spl[0]);
            result.setMsg(content.substring(spl[0].length() + 1));
        } else {
            result.setMsg(content);
        }
        return result;
    }

    private RespVerbatimString parseVerbatimString(Resp3Input line) throws Resp3ParseException {
        int length;
        try {
            length = parseInteger(line);
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid bulk string length", ex.isMissingData());
        }
        String content;
        if (length == -1) {
            return new RespVerbatimString();
        }
        if (line.length() < (length + 2)) {
            throw new Resp3ParseException("Unterminated end of verbatim string", true);
        }
        content = line.substring(length);
        var type = content.substring(0, 3);
        content = content.substring(4);
        if (line.charAtAndIncrement() != '\r') {
            throw new Resp3ParseException("Invalid bulk string format");
        }

        if (line.charAtAndIncrement() != '\n') {
            throw new Resp3ParseException("Invalid bulk string format");
        }
        var res = new RespVerbatimString();
        res.setType(type);
        res.setMsg(content);
        return res;
    }

    private List<Object> parseMap(Resp3Input line) throws Resp3ParseException {
        int length;
        try {
            length = parseInteger(line);
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid map length", ex.isMissingData());
        }
        List<Object> content = null;
        if (length == -1) {
            return content;
        }
        try {
            content = new ArrayList<>();
            content.add("@@MAP@@");
            for (var i = 0; i < length; i++) {
                var key = parse(line);
                var value = parse(line);
                content.add(List.of(key, value));
            }
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid map format", ex.isMissingData());
        }
        return content;
    }

    private List<Object> parseSet(Resp3Input line) throws Resp3ParseException {
        int length;
        try {
            length = parseInteger(line);
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid set length", ex.isMissingData());
        }
        List<Object> content = null;
        if (length == -1) {
            return content;
        }
        try {
            content = new ArrayList<>();
            content.add("@@SET@@");
            for (var i = 0; i < length; i++) {
                var key = parse(line);
                content.add(key);
            }
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid set format", ex.isMissingData());
        }
        return content;
    }

    public Object parse(String line) throws Resp3ParseException {
        var data = Resp3Input.of(line);
        return parse(data);
    }

    public Object parse(Resp3Input line) throws Resp3ParseException {

        char prefix = line.charAtAndIncrement();
        return switch (prefix) {
            case '+' -> parseSimpleString(line);
            case '-' -> parseSimpleError(line);
            case ':' -> parseInteger(line);
            case '$' -> parseBulkString(line);
            case '*' -> parseArray(line);
            case '_' -> {
                parseNull(line);
                yield null;
            }
            case '#' -> parseBool(line);
            case ',' -> parseDouble(line);
            case '(' -> parseBigNumber(line);
            case '!' -> parseBulkError(line);
            case '=' -> parseVerbatimString(line);
            case '%' -> parseMap(line);
            case '~' -> parseSet(line);
            case '>' -> parsePush(line);
            default -> throw new Resp3ParseException("Unknown response type: " + prefix);
        };


    }

    private RespPush parsePush(Resp3Input line) throws Resp3ParseException {
        int length;
        try {
            length = parseInteger(line);
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid push length", ex.isMissingData());
        }
        RespPush content = null;
        if (length == -1) {
            return content;
        }
        try {
            content = new RespPush();
            content.add("@@PUSH@@");
            for (var i = 0; i < length; i++) {
                var result = parse(line);
                content.add(result);
            }
        } catch (Resp3ParseException ex) {
            throw new Resp3ParseException("Invalid push format", ex.isMissingData());
        }
        return content;
    }

    @SuppressWarnings("DuplicateExpressions")
    public String serialize(JsonNode jsonNode) throws Resp3ParseException {
        StringBuilder result = new StringBuilder();
        if (jsonNode.isArray()) {
            var arrayNode = (ArrayNode) jsonNode;
            var type = "*";
            var size = 0;
            for (var item : arrayNode) {
                if (item.asText().equalsIgnoreCase("@@ARRAY@@")) {
                    type = "*";
                } else if (item.asText().equalsIgnoreCase("@@SET@@")) {
                    type = "~";
                } else if (item.asText().equalsIgnoreCase("@@MAP@@")) {
                    type = "%";
                } else if (item.asText().equalsIgnoreCase("@@PUSH@@")) {
                    type = ">";
                } else {
                    size++;
                }
            }
            result.append(type).append(size).append("\r\n");

            for (var item : arrayNode) {
                if (item.asText().equalsIgnoreCase("@@SET@@") || item.asText().equalsIgnoreCase("@@MAP@@") || item.asText().equalsIgnoreCase("@@PUSH@@") || item.asText().equalsIgnoreCase("@@ARRAY@@")) {
                    continue;
                }
                if (type.equalsIgnoreCase("%")) { //MAP
                    var subArray = (ArrayNode) item;
                    result.append(serialize(subArray.get(0)));
                    result.append(serialize(subArray.get(1)));
                } else {
                    result.append(serialize(item));
                }
            }
            return result.toString();
        } else if (jsonNode.isValueNode()) {
            var valNode = (ValueNode) jsonNode;
            if (valNode.isLong() || valNode.isInt() || valNode.isIntegralNumber() || valNode.isShort()) {
                return ":" + valNode.asInt() + "\r\n";
            } else if (valNode.isBoolean()) {
                return "#" + (valNode.asBoolean() ? "t" : "f") + "\r\n";
            } else if (valNode.isFloat() || valNode.isFloatingPointNumber() || valNode.isNumber()) {
                var floatValue = Float.parseFloat(valNode.asText());
                return "," + floatValue + "\r\n";
            } else if (valNode.isBigDecimal() || valNode.isDouble() || valNode.isFloat() || valNode.isFloatingPointNumber() || valNode.isNumber()) {
                var doubleValue = valNode.asDouble();
                if (doubleValue == Double.POSITIVE_INFINITY) {
                    return ",inf" + "\r\n";
                } else if (doubleValue == Double.NEGATIVE_INFINITY) {
                    return ",-inf" + "\r\n";
                }
                return "," + doubleValue + "\r\n";
            } else if (valNode.isBigInteger()) {
                return "(" + valNode.asText() + "\r\n";
            } else if (valNode.isNull()) {
                return "_" + "\r\n";
            } else if (valNode.isTextual()) {
                var text = valNode.asText();
                //if (text.contains("\r") || text.indexOf("\n") > 0) {
                return "$" + text.length() + "\r\n" + text + "\r\n";
                //} else {
                //    return "+" + text + "\r\n";
                //}
            }
        } else if (jsonNode.isObject()) {
            var objNode = (ObjectNode) jsonNode;
            var type = objNode.get("type").textValue();
            var msg = objNode.get("msg").textValue();
            if (type.equalsIgnoreCase("txt")) {
                return "=" + (msg.length() + 4) + "\r\n" + type + ":" + msg + "\r\n";
            } else if (type.equalsIgnoreCase("bin")) {
                return "=" + (msg.length() + 4) + "\r\n" + type + ":" + msg + "\r\n";
            } else {
                if (msg.contains("\r") || msg.indexOf("\n") > 0) {
                    return "!" + (msg.length() + 1 + type.length()) + "\r\n" + type + " " + msg + "\r\n";
                } else {
                    return "-" + type + " " + msg + "\r\n";
                }
            }
        }
        throw new Resp3ParseException("UNABLE TO RECOGNIZE TYPE");
    }
}
