package org.kendar.utils.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kendar.di.annotations.TpmService;
import org.kendar.utils.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TpmService
public class SimpleParser {
    private static final JsonMapper mapper = new JsonMapper();
    private static final Set<String> binaryOperator = Set.of("=", "<", ">",  "+", "-", "*", "%", "/", "!");
    private static final Set<FunctionDefinition> functionDefinitions = Set.of(
            new FunctionDefinition("CONCAT", -1),
            new FunctionDefinition("OR", -1),
            new FunctionDefinition("AND", -1),
            new FunctionDefinition("CONTAINS", 2),
            new FunctionDefinition("FILTER", 2),
            new FunctionDefinition("COUNT", 1),
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
            new FunctionDefinition("NULL", 0)
    );
    private final Pattern pattern = Pattern.compile("^\\d*\\.?\\d+$");

    public Token parse(String input) {
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
                        stack.peekLast().children.add(new Token(currentString.substring(1,currentString.length()-1), TokenType.STRING));
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
            throw new RuntimeException("Missing closing string");
        }
        if (parenthesisLevel < 0) {
            throw new RuntimeException("Missing opening bracket");
        }
        if (stack.size() > 1) {
            throw new RuntimeException("Missing closing bracket");
        }
        parseGeneric(currentString, stack);
        updateWithFunctions(token);
        optimizeBlocks(token);
        recognizeNumbers(token);

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
                            throw new RuntimeException("Missing function params for " + item.value);
                        }
                        var nextBlock = children.get(i + 1);
                        children.set(i + 1, null);
                        i++;
                        if (nextBlock.type != TokenType.BLOCK) {
                            throw new RuntimeException("Missing function params brackets for " + item.value);
                        }
                        item.children = adaptAsFunctionParams(nextBlock.children);
                    }
                }
            } else if (item.type == TokenType.BLOCK) {
                updateWithFunctions(item);
            }
        }
        token.children = children.stream().filter(f -> f != null).collect(Collectors.toList());
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
                            throw new RuntimeException("Missing function params for " + item.value);
                        }
                        var nextBlock = children.get(i + 1);
                        children.set(i + 1, null);
                        i++;
                        if (nextBlock.type != TokenType.BLOCK) {
                            throw new RuntimeException("Missing function params brackets for " + item.value);
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
        var prevChar=' ';
        for (int i = 0; i < charArray.length; i++) {
            var character = charArray[i];
            if (isBinaryOperator(character,prevChar)) {
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
            prevChar=character;
        }
        if (!currentString.isEmpty()) result.add(new Token(currentString,
                prevSpecial ? TokenType.OPERATOR : TokenType.VARIABLE));

        return result;
    }


    private boolean isBinaryOperator(char character, char prevChar) {
        if(prevChar=='[' && character=='*') return false;
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

    public Object evaluate(Token tokenMap, JsonNode testClass) {
        for (int i = 0; i < tokenMap.children.size(); i++) {
            var child = tokenMap.children.get(i);
            if(tokenMap.children.size()>(i+2)) {
                if(TokenType.OPERATOR==tokenMap.children.get(i + 1).type){
                    var operator = tokenMap.children.get(i + 1).value;
                    return execBinaryOperation(tokenMap.children.get(i), operator, tokenMap.children.get(i + 2), testClass);
                }
            }
            if (child.type == TokenType.FUNCTION) {
                return execFunction(child,testClass);
            } else if (child.type == TokenType.BLOCK) {
                return evaluate(child, testClass);
            }else if (child.type == TokenType.VARIABLE) {
                return convertToValue(child, testClass);
            }else { //binary operator
                if(tokenMap.children.size()>(i+2)) {
                    var operator = tokenMap.children.get(i + 1).value;
                    return execBinaryOperation(tokenMap.children.get(i), operator, tokenMap.children.get(i + 2), testClass);
                }else if (child.type == TokenType.STRING) {
                    return child.value;
                }else if (child.type == TokenType.NUMBER) {
                    return convertToValue(child,null);
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
            throw new RuntimeException("Unsupported binary operator for Strings: " + operator);
        } else if (rValue instanceof BigDecimal) {
            if (operator.equals("==")) {
                return lValue.equals(rValue);
            } else if (operator.equals("!=")) {
                return !lValue.equals(rValue);
            } else if (operator.equals(">")) {
                return ((BigDecimal) lValue).compareTo((BigDecimal) rValue) > 0;
            } else if (operator.equals(">=")) {
                return ((BigDecimal) lValue).compareTo((BigDecimal) rValue) > 0 ||
                        ((BigDecimal) lValue).compareTo((BigDecimal) rValue) == 0;
            } else if (operator.equals("<")) {
                return ((BigDecimal) lValue).compareTo((BigDecimal) rValue) < 0;
            } else if (operator.equals("<=")) {
                return ((BigDecimal) lValue).compareTo((BigDecimal) rValue) < 0 ||
                        ((BigDecimal) lValue).compareTo((BigDecimal) rValue) == 0;
            } else if (operator.equals("-")) {
                return ((BigDecimal) lValue).subtract((BigDecimal) rValue);
            } else if (operator.equals("+")) {
                return ((BigDecimal) lValue).add((BigDecimal) rValue);
            } else if (operator.equals("/")) {
                return ((BigDecimal) lValue).divide((BigDecimal) rValue);
            } else if (operator.equals("*")) {
                return ((BigDecimal) lValue).multiply((BigDecimal) rValue);
            } else if (operator.equals("%")) {
                return ((BigDecimal) lValue).remainder((BigDecimal) rValue);
            }
            throw new RuntimeException("Unsupported binary operator for Numbers: " + operator);
        } else if (rValue instanceof Boolean) {
            if (operator.equals("==")) {
                return lValue == rValue;
            } else if (operator.equals("!=")) {
                return lValue != rValue;
            }
            throw new RuntimeException("Unsupported binary operator for Booleans: " + operator);
        }
        return false;
    }

    private Object convertToValue(Token token, JsonNode testClass) {
        if (token.type == TokenType.STRING) {
            return token.value;
        } else if (token.type == TokenType.NUMBER) {
            return new BigDecimal(token.value);
        } else if (token.type == TokenType.FUNCTION) {
            return execFunction(token,testClass);
        }else if (token.type == TokenType.BLOCK) {
            return evaluate(token,testClass);
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
            if(obj.isArray()||obj.isObject())return obj;
            if (obj.isBigInteger() || obj.isLong() || obj.isInt() || obj.isShort())
                return BigDecimal.valueOf(obj.numberValue().longValue());
            if (obj.isFloat()) return BigDecimal.valueOf(obj.floatValue());
            if (obj.isBigDecimal()) return obj.decimalValue();
            if (obj.isDouble()) return BigDecimal.valueOf(obj.doubleValue());
            if (obj.isTextual()) return obj.textValue();
            if (obj.isBoolean()) return obj.booleanValue();
            throw new RuntimeException("Unsupported type: " + obj.getNodeType() + " for variable " + originalVariable + " on " + variable);
        }else{
            return convertToValue(originalVariable, remainder, obj);
        }

    }




    private Object execFunction(Token token,JsonNode testClass) {

        if (token.value.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (token.value.equalsIgnoreCase("false")) return Boolean.FALSE;

        var possibleFd = functionDefinitions.stream().filter(fd -> fd.name.equalsIgnoreCase(token.value)).findFirst().orElse(null);
        if (possibleFd == null) {
            throw new RuntimeException("Missing function " + token.value);
        }
        if(token.value.equalsIgnoreCase("and")){
            var result = true;
            for(var child:token.children){
                result = result && (boolean) convertToValue(child, testClass);
                if(!result)break;
            }
            return result;
        }else if(token.value.equalsIgnoreCase("or")){
            var result = false;
            for(var child:token.children){
                result = result || (boolean) convertToValue(child, testClass);
            }
            return result;
        }else if(token.value.equalsIgnoreCase("concat")){
            var result = "";
            for(var child:token.children){
                result  += convertToValue(child, testClass).toString();
            }
            return result;
        }else if(token.value.equalsIgnoreCase("count")){
            var value=convertToValue(token.children.get(0),testClass);
            if(value==null){
                return BigDecimal.valueOf(0);
            }
            if(JsonNode.class.isAssignableFrom(value.getClass())){
                var obOrArray = (JsonNode)value;

                if(!(obOrArray.isArray()||obOrArray.isObject()||obOrArray.isTextual())){
                    throw new RuntimeException("count parameter must be an object or an array");
                }
                return BigDecimal.valueOf(obOrArray.size());
            }else if(value instanceof String){
                return BigDecimal.valueOf(((String) value).length());
            }
            throw new RuntimeException("Unsupported type for count: " + value.getClass());

        }else if(token.value.equalsIgnoreCase("filter")){
            var value=convertToValue(token.children.get(0),testClass);
            var function=token.children.get(1);
            if(JsonNode.class.isAssignableFrom(value.getClass())){

                var obOrArray = (JsonNode)value;

                if(!(obOrArray.isArray()||obOrArray.isObject()||obOrArray.isTextual())){
                    throw new RuntimeException("count parameter must be an object or an array");
                }
                if(obOrArray.isArray()){
                    ArrayNode result = mapper.getMapper().createArrayNode();

                    for(var item:((ArrayNode)obOrArray)){
                        var objectNode = mapper.getMapper().createObjectNode();
                        objectNode.set("it",item);
                        if((boolean) convertToValue(function,(JsonNode) objectNode)){
                            result.add((JsonNode) item);
                        }
                    }
                    return result;
                }else if(obOrArray.isObject()){
                    ArrayNode result = mapper.getMapper().createArrayNode();
                    ObjectNode src = (ObjectNode) obOrArray;
                    var iterator = src.fields();
                    while(iterator.hasNext()){
                        var item = iterator.next();ObjectNode partialSubNode = mapper.getMapper().createObjectNode();
                        var textNodeKey = mapper.toJsonNode(item.getKey());
                        partialSubNode.set("value", (JsonNode) item.getValue());
                        partialSubNode.set("key",textNodeKey);
                        if((boolean) convertToValue(function,(JsonNode) partialSubNode)){
                            result.add((JsonNode) partialSubNode);
                        }
                    }
                    return result;
                }

                return obOrArray.size();
            }
            throw new RuntimeException("Unsupported type for filter: " + value.getClass());

        }else if(token.value.equalsIgnoreCase("contains")){
            var where = convertToValue(token.children.get(0), testClass).toString();
            var what = convertToValue(token.children.get(1), testClass).toString();
            return where.contains(what);
        }else if(token.value.equalsIgnoreCase("isnull")){
            return convertToValue(token.children.get(0), testClass)==null;
        }else if(token.value.equalsIgnoreCase("null")){
            return null;
        }else if(token.value.equalsIgnoreCase("isnotnull")){
            return convertToValue(token.children.get(0), testClass)!=null;
        }else if(token.value.equalsIgnoreCase("not")){
            return !(boolean) convertToValue(token.children.get(0),testClass);
        }
        return null;
    }
}
