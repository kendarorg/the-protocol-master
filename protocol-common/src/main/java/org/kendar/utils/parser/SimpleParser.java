package org.kendar.utils.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.ParserException;
import org.kendar.utils.JsonMapper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TpmService
public class SimpleParser {
    private static final ConcurrentHashMap<String, Token> tokensCache = new ConcurrentHashMap<>();
    private static final JsonMapper mapper = new JsonMapper();
    private static final Set<String> binaryOperator = Set.of("=", "<", ">", "+", "-", "*", "%", "/", "!");
    private static final Set<FunctionDefinition> functionDefinitions = Set.of(
            new FunctionDefinition("CONCAT", -1),
            new FunctionDefinition("OR", -1),
            new FunctionDefinition("AND", -1),
            new FunctionDefinition("CONTAINS", 2),
            new FunctionDefinition("FILTER", 2),
            new FunctionDefinition("SUBSTR", 2),
            new FunctionDefinition("WRAP", 2),
            new FunctionDefinition("COUNT", 1),
            new FunctionDefinition("MSTODATE", 1),
            /*new FunctionDefinition("NOW", 0),
            new FunctionDefinition("MSTODATE", 1),
            new FunctionDefinition("NANOTODATE", 1),
            new FunctionDefinition("STRTODATE", 1),*/
            new FunctionDefinition("ISNULL", 1),
            new FunctionDefinition("ISNOTNULL", 1),
            //new FunctionDefinition("IN", -1),
            new FunctionDefinition("NOT", 1),
            new FunctionDefinition("TRUE", 0),
            new FunctionDefinition("FALSE", 0),
            new FunctionDefinition("NULL", 0),
            new FunctionDefinition("SELECT", -1),
            new FunctionDefinition("WHAT", -1),//SELECT(WHAT,WHERE,GROUP)
            new FunctionDefinition("ORDERBY", -1),
            new FunctionDefinition("GROUPBY", -1),
            new FunctionDefinition("WHERE", 1),
            new FunctionDefinition("MAX", 1),
            new FunctionDefinition("MIN", 1),
            new FunctionDefinition("AVG", 1),
            new FunctionDefinition("SUM", 1),
            new FunctionDefinition("ASC", 1),
            new FunctionDefinition("DESC", 1)
    );


    private final Pattern pattern = Pattern.compile("^\\d*\\.?\\d+$");

    private static List<String> splitStringBySize(String str, int size) {
        ArrayList<String> split = new ArrayList<>();
        for (int i = 0; i <= str.length() / size; i++) {
            split.add(str.substring(i * size, Math.min((i + 1) * size, str.length())));
        }
        return split;
    }

    public static String convertTime(long time) {
        var date = new Date(time);
        var format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return format.format(date);
    }

    public Token parse(String input) {
        if (tokensCache.containsKey(input)) {
            return tokensCache.get(input);
        }
        var token = new Token(TokenType.BLOCK);
        char[] charArray = input.toCharArray();
        var inString = false;
        var stack = new LinkedList<Token>();
        stack.push(token);
        var parenthesisLevel = 0;
        var prevSlash = false;
        var currentString = "";
        for (int i = 0; i < charArray.length; i++) {
            var character = charArray[i];
            if (inString) {
                if (character == '\\') {
                    prevSlash = true;
                    currentString += character;
                } else if (character == '\'') {
                    if (prevSlash) {
                        prevSlash = false;
                        currentString += character;
                    } else {
                        currentString += character;
                        inString = false;
                        stack.peekLast().children.add(new Token(currentString.substring(1, currentString.length() - 1), TokenType.STRING));
                        currentString = "";
                    }
                } else {
                    currentString += character;
                }
            } else {
                if (character == ',') {
                    parseGeneric(currentString, stack);
                    addToken(stack, ",", TokenType.COMMA);
                    currentString = "";
                } else if (character == '\'') {
                    inString = true;
                    parseGeneric(currentString, stack);
                    currentString = "" + character;
                } else if (character == ' ' || character == '\n' || character == '\r' || character == '\f' || character == '\t') {
                    parseGeneric(currentString, stack);
                    currentString = "";
                } else if (character == '(') {
                    parenthesisLevel++;
                    parseGeneric(currentString, stack);
                    var blokTocken = addToken(stack, null, TokenType.BLOCK);
                    stack.add(blokTocken);
                    currentString = "";
                } else if (character == ')') {
                    parenthesisLevel--;
                    parseGeneric(currentString, stack);
                    stack.removeLast();
                    currentString = "";
                } else {
                    currentString += character;
                }
            }
        }
        if (inString) {
            throw new ParserException("Missing closing string");
        }
        if (parenthesisLevel < 0) {
            throw new ParserException("Missing opening bracket");
        }
        if (stack.size() > 1) {
            throw new ParserException("Missing closing bracket");
        }
        parseGeneric(currentString, stack);
        updateWithFunctions(token);
        optimizeBlocks(token);
        recognizeNumbers(token);
        if (token.type == TokenType.BLOCK && token.children.size() == 1 && token.children.get(0).value.equalsIgnoreCase("select")) {
            return token.children.get(0);
        }
        tokensCache.put(input, token);
        return token;
    }

    private void recognizeNumbers(Token token) {
        for (var child : token.children) {
            if (child.type == TokenType.VARIABLE) {
                if (pattern.matcher(child.value).matches()) {
                    child.type = TokenType.NUMBER;
                }
            } else {
                recognizeNumbers(child);
            }
        }
    }

    private Token addToken(LinkedList<Token> stack, String s, TokenType tokenType) {
        var token = new Token(s, tokenType);
        stack.peekLast().children.add(token);
        return token;
    }

    private void parseGeneric(String currentString, LinkedList<Token> stack) {
        if (!currentString.isEmpty()) stack.peekLast().children.addAll(parseGeneric(currentString));
    }

    private void updateWithFunctions(Token token) {
        List<Token> children = token.children;
        for (int i = 0; i < children.size(); i++) {
            var item = children.get(i);
            if (item == null) continue;
            if (item.type == TokenType.VARIABLE) {

                var possibleFd = functionDefinitions.stream().filter(fd -> fd.name.equalsIgnoreCase(item.value)).findFirst().orElse(null);
                if (possibleFd != null) {
                    item.type = TokenType.FUNCTION;
                    if (possibleFd.paramsCount != 0) {
                        if ((i + 1) >= children.size()) {
                            throw new ParserException("Missing function params for " + item.value);
                        }
                        var nextBlock = children.get(i + 1);
                        children.set(i + 1, null);
                        i++;
                        if (nextBlock.type != TokenType.BLOCK) {
                            throw new ParserException("Missing function params brackets for " + item.value);
                        }
                        item.children = adaptAsFunctionParams(nextBlock.children);
                    }
                }
            } else if (item.type == TokenType.BLOCK) {
                updateWithFunctions(item);
            }
        }
        token.children = children.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Token> adaptAsFunctionParams(List<Token> children) {
        var result = new ArrayList<Token>();
        var lastToken = new Token(TokenType.BLOCK);
        for (int i = 0; i < children.size(); i++) {
            var item = children.get(i);
            if (item.type == TokenType.COMMA) {
                result.add(lastToken);
                lastToken = new Token(TokenType.BLOCK);
            } else if (item.type == TokenType.VARIABLE) {
                var possibleFd = functionDefinitions.stream().filter(fd -> fd.name.equalsIgnoreCase(item.value)).findFirst().orElse(null);
                if (possibleFd != null) {
                    item.type = TokenType.FUNCTION;
                    if (possibleFd.paramsCount != 0) {
                        if ((i + 1) >= children.size()) {
                            throw new ParserException("Missing function params for " + item.value);
                        }
                        var nextBlock = children.get(i + 1);
                        children.set(i + 1, null);
                        i++;
                        if (nextBlock.type != TokenType.BLOCK) {
                            throw new ParserException("Missing function params brackets for " + item.value);
                        }
                        item.children = adaptAsFunctionParams(nextBlock.children);
                    }
                }
                lastToken.children.add(item);
            } else {
                lastToken.children.add(item);
            }

        }
        result.add(lastToken);
        for (int i = children.size() - 1; i >= 0; i--) {
            var child = children.get(i);
            if (child == null) children.remove(i);

        }
        return result;
    }

    private List<Token> parseGeneric(String input) {
        var result = new ArrayList<Token>();
        char[] charArray = input.toCharArray();
        var currentString = "";
        var prevSpecial = false;
        var prevChar = ' ';
        for (int i = 0; i < charArray.length; i++) {
            var character = charArray[i];
            if (isBinaryOperator(character, prevChar)) {
                if (prevSpecial) {
                    currentString += character;
                } else {
                    if (!currentString.isEmpty()) result.add(new Token(currentString, TokenType.VARIABLE));
                    prevSpecial = true;
                    currentString = "" + character;
                }
            } else if (prevSpecial) {
                if (!currentString.isEmpty()) result.add(new Token(currentString, TokenType.OPERATOR));
                prevSpecial = false;
                currentString = "" + character;
            } else {
                currentString += character;
            }
            prevChar = character;
        }
        if (!currentString.isEmpty()) result.add(new Token(currentString,
                prevSpecial ? TokenType.OPERATOR : TokenType.VARIABLE));

        return result;
    }

    private boolean isBinaryOperator(char character, char prevChar) {
        if (prevChar == '[' && character == '*') return false;
        return binaryOperator.contains(String.valueOf(character));
    }

    private void optimizeBlocks(Token token) {
        for (var child : token.children) {
            if (child.type == TokenType.BLOCK) {
                while (child.type == TokenType.BLOCK && child.children.size() == 1) {
                    child.type = child.children.get(0).type;
                    child.value = child.children.get(0).value;
                    child.children = child.children.get(0).children;
                    optimizeBlocks(child);
                }
            } else if (child.type == TokenType.FUNCTION) {
                optimizeBlocks(child);
            }
        }
    }

    public ArrayNode select(Token select, ArrayNode inputArray) {
        var resultArray = mapper.getMapper().createArrayNode();
        if (!select.value.equalsIgnoreCase("select")) {
            throw new ParserException("Input must start with 'select()'");
        }
        var what = select.children.stream().filter(child -> child.value != null && child.value.equalsIgnoreCase("what")).findFirst().orElse(null);
        var where = select.children.stream().filter(child -> child.value != null && child.value.equalsIgnoreCase("where")).findFirst().orElse(null);
        var group = select.children.stream().filter(child -> child.value != null && child.value.equalsIgnoreCase("groupby")).findFirst().orElse(null);
        var order = select.children.stream().filter(child -> child.value != null && child.value.equalsIgnoreCase("orderby")).findFirst().orElse(null);
        resultArray = applyWhere(where, inputArray, resultArray);

        resultArray = applyProjectionWithoutGrouping(what, group, resultArray);
        resultArray = applyGrouping(group, resultArray, what);
        resultArray = applyOrder(order, resultArray);
        return resultArray;
    }

    private ArrayNode applyWhere(Token where, ArrayNode inputArray, ArrayNode resultArray) {
        if (where != null) {
            for (var item : inputArray) {
                if ((boolean) evaluate(where, item)) {
                    resultArray.add(item);
                }
            }
        } else {
            resultArray = inputArray;
        }
        return resultArray;
    }

    public Object evaluate(Token tokenMap, JsonNode testClass) {
        for (int i = 0; i < tokenMap.children.size(); i++) {
            var child = tokenMap.children.get(i);
            if (tokenMap.children.size() > (i + 2)) {
                if (TokenType.OPERATOR == tokenMap.children.get(i + 1).type) {
                    var operator = tokenMap.children.get(i + 1).value;
                    return execBinaryOperation(tokenMap.children.get(i), operator, tokenMap.children.get(i + 2), testClass);
                }
            }
            if (child.type == TokenType.FUNCTION) {
                return execFunction(child, testClass);
            } else if (child.type == TokenType.BLOCK) {
                return evaluate(child, testClass);
            } else if (child.type == TokenType.VARIABLE) {
                return convertToValue(child, testClass);
            } else { //binary operator
                if (tokenMap.children.size() > (i + 2)) {
                    var operator = tokenMap.children.get(i + 1).value;
                    return execBinaryOperation(tokenMap.children.get(i), operator, tokenMap.children.get(i + 2), testClass);
                } else if (child.type == TokenType.STRING) {
                    return child.value;
                } else if (child.type == TokenType.NUMBER) {
                    return convertToValue(child, null);
                }
            }
        }
        return null;
    }

    private Object execBinaryOperation(Token left, String operator, Token right, JsonNode testClass) {
        var lValue = convertToValue(left, testClass);
        var rValue = convertToValue(right, testClass);
        if (lValue == null && rValue == null) {
            if (operator.equals("==")) return Boolean.TRUE;
            return Boolean.FALSE;
        } else if ((lValue == null && rValue != null) || (lValue != null && lValue == null)) {
            if (operator.equals("==")) return Boolean.FALSE;
            if (operator.equals("!=")) return Boolean.TRUE;
            return Boolean.FALSE;
        }

        if (lValue instanceof String) {
            if (operator.equals("==")) {
                return lValue.equals(rValue);
            } else if (operator.equals("!=")) {
                return !lValue.equals(rValue);
            }
            throw new ParserException("Unsupported binary operator for Strings: " + operator);
        } else if (rValue instanceof BigDecimal) {
            return switch (operator) {
                case "==" -> lValue.equals(rValue);
                case "!=" -> !lValue.equals(rValue);
                case ">" -> ((BigDecimal) lValue).compareTo((BigDecimal) rValue) > 0;
                case ">=" -> ((BigDecimal) lValue).compareTo((BigDecimal) rValue) > 0 ||
                        ((BigDecimal) lValue).compareTo((BigDecimal) rValue) == 0;
                case "<" -> ((BigDecimal) lValue).compareTo((BigDecimal) rValue) < 0;
                case "<=" -> ((BigDecimal) lValue).compareTo((BigDecimal) rValue) < 0 ||
                        ((BigDecimal) lValue).compareTo((BigDecimal) rValue) == 0;
                case "-" -> ((BigDecimal) lValue).subtract((BigDecimal) rValue);
                case "+" -> ((BigDecimal) lValue).add((BigDecimal) rValue);
                case "/" -> ((BigDecimal) lValue).divide((BigDecimal) rValue);
                case "*" -> ((BigDecimal) lValue).multiply((BigDecimal) rValue);
                case "%" -> ((BigDecimal) lValue).remainder((BigDecimal) rValue);
                default -> throw new ParserException("Unsupported binary operator for Numbers: " + operator);
            };
        } else if (rValue instanceof Boolean) {
            if (operator.equals("==")) {
                return lValue == rValue;
            } else if (operator.equals("!=")) {
                return lValue != rValue;
            }
            throw new ParserException("Unsupported binary operator for Booleans: " + operator);
        }
        return false;
    }

    private Object convertToValue(Token token, JsonNode testClass) {
        if (token.type == TokenType.STRING) {
            return token.value;
        } else if (token.type == TokenType.NUMBER) {
            return new BigDecimal(token.value);
        } else if (token.type == TokenType.FUNCTION) {
            return execFunction(token, testClass);
        } else if (token.type == TokenType.BLOCK) {
            return evaluate(token, testClass);
        } else if (token.type == TokenType.VARIABLE) {
            return convertToValue(token.value, token.value, testClass);
        }
        return null;
    }

    private Object convertToValue(String originalVariable, String variable, JsonNode testClass) {
        var oldArray = variable.split("\\.", 2);
        var currentPart = oldArray[0];
        String remainder = null;
        if (oldArray.length > 1) {
            remainder = oldArray[1];
        }
        var obj = testClass.get(currentPart);
        if (obj == null) {
            return null;
        }
        if (remainder == null) {
            if (obj.isArray() || obj.isObject()) return obj;
            if (obj.isBigInteger() || obj.isLong() || obj.isInt() || obj.isShort())
                return BigDecimal.valueOf(obj.numberValue().longValue());
            if (obj.isFloat()) return BigDecimal.valueOf(obj.floatValue());
            if (obj.isBigDecimal()) return obj.decimalValue();
            if (obj.isDouble()) return BigDecimal.valueOf(obj.doubleValue());
            if (obj.isTextual()) return obj.textValue();
            if (obj.isBoolean()) return obj.booleanValue();
            throw new ParserException("Unsupported type: " + obj.getNodeType() + " for variable " + originalVariable + " on " + variable);
        } else {
            return convertToValue(originalVariable, remainder, obj);
        }

    }

    private Object execFunction(Token token, JsonNode testClass) {

        if (token.value.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (token.value.equalsIgnoreCase("false")) return Boolean.FALSE;

        var possibleFd = functionDefinitions.stream().filter(fd -> fd.name.equalsIgnoreCase(token.value)).findFirst().orElse(null);
        if (possibleFd == null) {
            throw new ParserException("Missing function " + token.value);
        }
        if (token.value.equalsIgnoreCase("and")) {
            var result = true;
            for (var child : token.children) {
                result = result && (boolean) convertToValue(child, testClass);
                if (!result) break;
            }
            return result;
        } else if (token.value.equalsIgnoreCase("or")) {
            var result = false;
            for (var child : token.children) {
                result = result || (boolean) convertToValue(child, testClass);
            }
            return result;
        } else if (token.value.equalsIgnoreCase("concat")) {
            var result = "";
            for (var child : token.children) {
                result += convertToValue(child, testClass).toString();
            }
            return result;
        } else if (token.value.equalsIgnoreCase("count")) {
            return parseCount(token, testClass);

        } else if (token.value.equalsIgnoreCase("filter")) {
            return parseFilter(token, testClass);

        } else if (token.value.equalsIgnoreCase("substr")) {
            var val = convertToValue(token.children.get(0), testClass);
            var what = "";
            if (val instanceof ObjectNode || val instanceof ArrayNode) {
                what = mapper.serialize(val);
            } else {
                what = val.toString();
            }
            var length = Integer.parseInt(convertToValue(token.children.get(1), testClass).toString());
            if (what.length() < length) return what;
            var fullLenght = what.length();
            return what.substring(0, length) + "...(" + fullLenght + ")";

        } else if (token.value.equalsIgnoreCase("wrap")) {
            var val = convertToValue(token.children.get(0), testClass);
            var what = "";
            if (val instanceof ObjectNode || val instanceof ArrayNode) {
                what = mapper.serialize(val);
            } else {
                what = val.toString();
            }
            var length = Integer.parseInt(convertToValue(token.children.get(1), testClass).toString());
            var wrapItem = convertToValue(token.children.get(2), testClass).toString();
            return String.join(wrapItem, splitStringBySize(what, length));

        } else if (token.value.equalsIgnoreCase("contains")) {
            var where = convertToValue(token.children.get(0), testClass).toString();
            var what = convertToValue(token.children.get(1), testClass).toString();
            return where.contains(what);
        } else if (token.value.equalsIgnoreCase("isnull")) {
            return convertToValue(token.children.get(0), testClass) == null;
        } else if (token.value.equalsIgnoreCase("null")) {
            return null;
        } else if (token.value.equalsIgnoreCase("isnotnull")) {
            return convertToValue(token.children.get(0), testClass) != null;
        } else if (token.value.equalsIgnoreCase("not")) {
            return !(boolean) convertToValue(token.children.get(0), testClass);
        } else if (token.value.equalsIgnoreCase("mstodate")) {
            return convertTime(((BigDecimal) convertToValue(token.children.get(0), testClass)).longValue());
        }
        return null;
    }

    private BigDecimal parseCount(Token token, JsonNode testClass) {
        var value = convertToValue(token.children.get(0), testClass);
        if (value == null) {
            return BigDecimal.valueOf(0);
        }
        if (JsonNode.class.isAssignableFrom(value.getClass())) {
            var obOrArray = (JsonNode) value;

            if (!(obOrArray.isArray() || obOrArray.isObject() || obOrArray.isTextual())) {
                throw new ParserException("count parameter must be an object or an array");
            }
            return BigDecimal.valueOf(obOrArray.size());
        } else if (value instanceof String) {
            return BigDecimal.valueOf(((String) value).length());
        }
        throw new ParserException("Unsupported type for count: " + value.getClass());
    }

    private Object parseFilter(Token token, JsonNode testClass) {
        var value = convertToValue(token.children.get(0), testClass);
        var function = token.children.get(1);
        if (JsonNode.class.isAssignableFrom(value.getClass())) {

            var obOrArray = (JsonNode) value;

            if (!(obOrArray.isArray() || obOrArray.isObject() || obOrArray.isTextual())) {
                throw new ParserException("count parameter must be an object or an array");
            }
            if (obOrArray.isArray()) {
                ArrayNode result = mapper.getMapper().createArrayNode();

                for (var item : obOrArray) {
                    var objectNode = mapper.getMapper().createObjectNode();
                    objectNode.set("it", item);
                    if ((boolean) convertToValue(function, objectNode)) {
                        result.add(item);
                    }
                }
                return result;
            } else if (obOrArray.isObject()) {
                ArrayNode result = mapper.getMapper().createArrayNode();
                ObjectNode src = (ObjectNode) obOrArray;
                var iterator = src.fields();
                while (iterator.hasNext()) {
                    var item = iterator.next();
                    ObjectNode partialSubNode = mapper.getMapper().createObjectNode();
                    var textNodeKey = mapper.toJsonNode(item.getKey());
                    partialSubNode.set("value", item.getValue());
                    partialSubNode.set("key", textNodeKey);
                    if ((boolean) convertToValue(function, partialSubNode)) {
                        result.add(partialSubNode);
                    }
                }
                return result;
            }

            return obOrArray.size();
        }
        throw new ParserException("Unsupported type for filter: " + value.getClass());
    }

    public List<JsonNode> sort(List<JsonNode> list, String sortString) {
        var sortItems = sortString.split(",");
        var sortExpression = new ArrayList<SortSpec>();
        for (int i = 0; i < sortItems.length; i++) {
            var sortItem = sortItems[i].trim();
            var splItem = sortItem.split("\\s+");
            var ss = new SortSpec();
            ss.field = splItem[0];
            if (splItem.length == 1) {
                ss.ascending = true;
            } else {
                ss.ascending = splItem[1].equalsIgnoreCase("ASC");
            }
            sortExpression.add(ss);
        }
        return list;
    }

    private ArrayNode applyGrouping(Token group, ArrayNode resultArray, Token what) {
        if (group != null) {
            var mapGroup = new HashMap<String, List<JsonNode>>();
            var toGroupArray = resultArray;
            resultArray = mapper.getMapper().createArrayNode();
            //Load all items that should be grouped
            buildGroups(group, toGroupArray, mapGroup);
            if (what == null) {
                buildGroupWithoutProjection(group, resultArray, mapGroup);
            } else {
                buildGroupWithProjection(what, group, resultArray, mapGroup);
            }
        }
        return resultArray;
    }

    private void buildGroupWithoutProjection(Token group, ArrayNode resultArray, HashMap<String, List<JsonNode>> mapGroup) {
        for (var item : mapGroup.values()) {
            var objectNode = mapper.getMapper().createObjectNode();
            var groupItem = item.get(0);
            for (var whatValue : group.children) {
                var value = convertToValue(whatValue, groupItem);
                var jsonValue = mapper.convertValue(value);
                objectNode.set(whatValue.value, jsonValue);
            }
            resultArray.add(objectNode);
        }
    }

    private void buildGroups(Token group, ArrayNode toGroupArray, HashMap<String, List<JsonNode>> mapGroup) {
        for (var item : toGroupArray) {
            var groupKey = "";
            for (var whatValue : group.children) {
                if (whatValue.type != TokenType.VARIABLE) {
                    throw new ParserException("Only variables can be grouped with 'groupby'");
                }
                var value = convertToValue(whatValue, item);
                var jsonValue = mapper.convertValue(value);
                groupKey += jsonValue.toString();
            }
            if (!mapGroup.containsKey(groupKey)) {
                mapGroup.put(groupKey, new ArrayList<>());
            }
            mapGroup.get(groupKey).add(item);
        }
    }

    private ArrayNode applyProjectionWithoutGrouping(Token what, Token group, ArrayNode resultArray) {
        if (what != null && group == null) {
            var toProjectArray = resultArray;
            resultArray = mapper.getMapper().createArrayNode();
            for (var item : toProjectArray) {
                var objectNode = mapper.getMapper().createObjectNode();
                for (var whatValue : what.children) {
                    if (whatValue.children.isEmpty()) {
                        var value = convertToValue(whatValue, item);
                        var jsonValue = mapper.convertValue(value);
                        objectNode.set(whatValue.value, jsonValue);
                    } else {
                        if (!whatValue.children.get(1).value.equalsIgnoreCase("=")) {
                            throw new ParserException("Should assign with '='");
                        }
                        var value = convertToValue(whatValue.children.get(2), item);
                        var jsonValue = mapper.convertValue(value);
                        objectNode.set(whatValue.children.get(0).value, jsonValue);
                    }
                }
                resultArray.add(objectNode);
            }
        }
        return resultArray;
    }

    private void buildGroupWithProjection(Token what, Token group, ArrayNode resultArray, HashMap<String, List<JsonNode>> mapGroup) {
        var avgFields = new HashSet<String>();
        for (var groupItems : mapGroup.values()) {
            var objectNode = new HashMap<String, Object>();
            for (var groupItem : groupItems) {
                for (var whatValue : what.children) {
                    var lvalue = whatValue.children.get(0).value;
                    if (!whatValue.children.get(1).value.equalsIgnoreCase("=")) {
                        throw new ParserException("Should assign with '='");
                    }
                    var rvalue = whatValue.children.get(2);
                    if (rvalue.value.equalsIgnoreCase("count")) {
                        if (objectNode.get(lvalue) == null) {
                            objectNode.put(lvalue, 1L);
                        } else {
                            objectNode.put(lvalue, (long) objectNode.get(lvalue) + 1L);
                        }
                    } else if (rvalue.value.equalsIgnoreCase("sum") || rvalue.value.equalsIgnoreCase("avg")) {
                        if (rvalue.value.equalsIgnoreCase("avg")) {
                            avgFields.add(lvalue);
                        }
                        if (objectNode.get(lvalue) == null) {
                            objectNode.put(lvalue, convertToValue(rvalue.children.get(0), groupItem));
                        } else {
                            var value = ((BigDecimal) objectNode.get(lvalue)).add((BigDecimal) convertToValue(rvalue.children.get(0), groupItem));
                            objectNode.put(lvalue, value);
                        }
                    } else if (rvalue.value.equalsIgnoreCase("min") || rvalue.value.equalsIgnoreCase("max")) {
                        if (objectNode.get(lvalue) == null) {
                            objectNode.put(lvalue, convertToValue(rvalue.children.get(0), groupItem));
                        } else {
                            var prevValue = ((BigDecimal) objectNode.get(lvalue));
                            var currValue = (BigDecimal) convertToValue(rvalue.children.get(0), groupItem);
                            if (rvalue.value.equalsIgnoreCase("min")) {
                                if (currValue.compareTo(prevValue) < 0) {
                                    objectNode.put(lvalue, currValue);
                                }
                            } else {
                                if (currValue.compareTo(prevValue) > 0) {
                                    objectNode.put(lvalue, currValue);
                                }
                            }
                        }
                    }
                }
            }
            var groupItem = groupItems.get(0);
            for (var whatValue : group.children) {
                var value = convertToValue(whatValue, groupItem);
                objectNode.put(whatValue.value, value);
            }
            for (var avgField : avgFields) {
                var value = (BigDecimal) objectNode.get(avgField);
                objectNode.put(avgField, value.divide(BigDecimal.valueOf(groupItems.size())));
            }
            resultArray.add(mapper.toJsonNode(objectNode));
        }
    }

    private ArrayNode applyOrder(Token order, ArrayNode inputArray) {
        ArrayNode resultArray = mapper.getMapper().createArrayNode();
        if (order != null) {
            var toSort = new ArrayList<JsonNode>();
            var resIt = inputArray.iterator();
            while (resIt.hasNext()) {
                toSort.add(resIt.next());
            }
            toSort.sort((o1, o2) -> {
                var compareFunctions = new ArrayList<>(order.children);
                while (!compareFunctions.isEmpty()) {
                    var compareFunction = compareFunctions.remove(0);
                    var asc = compareFunction.value.equalsIgnoreCase("ASC");

                    var o1Value = convertToValue(compareFunction.children.get(0), o1);
                    var o2Value = convertToValue(compareFunction.children.get(0), o2);
                    if (o1Value == null && o2Value == null) continue;
                    if (o1Value == null && o2Value != null) {
                        if (asc) return 1;
                        return -1;
                    }
                    if (o1Value != null && o2Value == null) {
                        if (asc) return -1;
                        return 1;
                    }
                    if (o1Value instanceof String) {
                        var result = ((String) o1Value).compareTo((String) o2Value);
                        if (result == 0) continue;
                        if (asc) return result;
                        return -result;
                    }
                    if (o1Value instanceof BigDecimal) {
                        var result = ((BigDecimal) o1Value).compareTo((BigDecimal) o2Value);
                        if (result == 0) continue;
                        if (asc) return result;
                        return -result;
                    }
                }
                return 0;
            });

            for (var item : toSort) {
                resultArray.add(item);
            }
        } else {
            resultArray = inputArray;
        }
        return resultArray;
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    class SortSpec {
        public String field;
        public boolean ascending;
    }
}
