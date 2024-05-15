package org.kendar.redis.parser;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

import java.io.BufferedReader;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;


public class Resp3Parser {
    private BufferedReader reader;
    /**
     * The following contains all rules
     */
    Protocol protocol;
    Connection connection;
    RedisInputStream input;
    RedisOutputStream output;

    static boolean isUpper(String input) {
        for (char c : input.toCharArray()) {
            //  don't write in this way: if (!Character.isUpperCase(c))
            if (Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }
    private RespError parseSimpleError(Resp3Input line) throws Resp3ParseException {
        var content = parseSimpleString(line);

        var spl = content.split("\\s");
        var result = new RespError();
        if(isUpper(spl[0])){
            result.setType(spl[0]);
            result.setMsg(content.substring(spl[0].length()+1));
        }else{
            result.setMsg(content);
        }
        return result;
    }


    private String parseSimpleString(Resp3Input line) throws Resp3ParseException{
        String result = "";
        var end = 0;
        while(line.hasNext() && end!=2){
            var ch = line.charAtAndIncrement();
            if (ch=='\r' && end==0){
                end++;
            }else if (ch=='\n' && end==1){
                end++;
            }else if( ch!='\r' && ch!='\n' && end==0){
                result+=ch;
            }else{
                throw new Resp3ParseException("Invalid string format");
            }
        }
        if(end!=2){
            throw new Resp3ParseException("Unterminated end of string",true);
        }
        return result;
    }

    private int parseInteger(Resp3Input line) throws Resp3ParseException {
        String result = "";
        var end = 0;
        while(line.hasNext() && end!=2){
            var ch = line.charAtAndIncrement();
            if (ch=='\r' && end==0){
                end++;
            }else if (ch=='\n' && end==1){
                end++;
            }else if( ((ch >= '0' && ch <= '9')|| ch=='-' || ch=='+') && end==0){
                result+=ch;
            }else{
                throw new Resp3ParseException("Invalid integer format");
            }
        }
        if(end!=2){
            throw new Resp3ParseException("Unterminated end of integer",true);
        }
        try {
            return Integer.parseInt(result);
        }catch (Exception ex){
            throw new Resp3ParseException("Invalid integer");
        }
    }

    private String parseBulkString(Resp3Input line) throws Resp3ParseException {
        int length = -1;
        try{
            length = parseInteger(line);
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid bulk string length",ex.isMissingData());
        }
        String content = null;
        if(length==-1) {
            return content;
        }
        if (line.length() < (length + 2)) {
            throw new Resp3ParseException("Unterminated end of bulk string", true);
        }
        content = line.substring(length);
        if(line.charAtAndIncrement()!='\r'){
            throw new Resp3ParseException("Invalid bulk string format");
        }

        if(line.charAtAndIncrement()!='\n'){
            throw new Resp3ParseException("Invalid bulk string format");
        }
        return content;
    }

    private List<Object> parseArray(Resp3Input line) throws Resp3ParseException {
        int length = -1;
        try{
            length = parseInteger(line);
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid bulk string length",ex.isMissingData());
        }
        List<Object> content = null;
        if(length==-1) {
            return content;
        }
        try {
            content = new ArrayList<Object>();
            for (var i = 0; i < length; i++) {
                var result = parse(line);
                content.add(result);
            }
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid array format",ex.isMissingData());
        }
        return content;
    }

    private Object parseNull(Resp3Input line) throws Resp3ParseException {

        if(line.charAtAndIncrement()!='\r'){
            throw new Resp3ParseException("Invalid null format");
        }

        if(line.charAtAndIncrement()!='\n'){
            throw new Resp3ParseException("Invalid null format");
        }
        return null;
    }

    private boolean parseBool(Resp3Input line) throws Resp3ParseException {
        var letter = line.charAtAndIncrement();
        if(letter!='t' && letter!='f'){
            throw new Resp3ParseException("Invalid bool format string");
        }
        if(line.charAtAndIncrement()!='\r'){
            throw new Resp3ParseException("Invalid bool format");
        }

        if(line.charAtAndIncrement()!='\n'){
            throw new Resp3ParseException("Invalid bool format");
        }
        return letter=='t';
    }



    private BigInteger parseBigNumber(Resp3Input line) throws Resp3ParseException {
        String result = "";
        var end = 0;
        while(line.hasNext() && end!=2){
            var ch = line.charAtAndIncrement();
            if (ch=='\r' && end==0){
                end++;
            }else if (ch=='\n' && end==1){
                end++;
            }else if( ((ch >= '0' && ch <= '9')|| ch=='-' || ch=='+') && end==0){
                result+=ch;
            }else{
                throw new Resp3ParseException("Invalid integer format");
            }
        }
        if(end!=2){
            throw new Resp3ParseException("Unterminated end of integer",true);
        }
        try {
            return new BigInteger(result);
        }catch (Exception ex){
            throw new Resp3ParseException("Invalid integer");
        }
    }

    private double parseDouble(Resp3Input line) throws Resp3ParseException {

        String result = "";
        var end = 0;
        while(line.hasNext() && end!=2){
            var ch = line.charAtAndIncrement();
            if (ch=='\r' && end==0){
                end++;
            }else if (ch=='\n' && end==1){
                end++;
            }else if( (ch=='-' || ch=='+' || ch=='.' || ch=='e'
                    || ch=='i' || ch=='n' || ch=='f' || ch=='a'
                    || ch=='E'  || (ch>='0' && ch<='9')) && end==0){
                result+=ch;
            }else{
                throw new Resp3ParseException("Invalid integer format");
            }
        }
        if(end!=2){
            throw new Resp3ParseException("Unterminated end of integer",true);
        }
        if(result.equalsIgnoreCase("inf")){
            return Double.POSITIVE_INFINITY;
        }else if(result.equalsIgnoreCase("-inf")){
            return Double.NEGATIVE_INFINITY;
        }else if(result.equalsIgnoreCase("nan")){
            return Float.NaN;
        }
        var pattern = Pattern.compile("([+\\-]?[0-9\\.]+)([Ee])?([\\-+]?[0-9]*)");
        var matcher = pattern.matcher(result);
        if(!matcher.find()){
            throw new Resp3ParseException("Invalid double format");
        }
        return Double.parseDouble(result);
    }

    private RespError parseBulkError(Resp3Input line) throws Resp3ParseException {
        var content = parseBulkString(line);

        var spl = content.split("\\s");
        var result = new RespError();
        if(isUpper(spl[0])){
            result.setType(spl[0]);
            result.setMsg(content.substring(spl[0].length()+1));
        }else{
            result.setMsg(content);
        }
        return result;
    }

    private RespVerbatimString parseVerbatimString(Resp3Input line) throws Resp3ParseException {
        int length = -1;
        try{
            length = parseInteger(line);
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid bulk string length",ex.isMissingData());
        }
        String content = null;
        if(length==-1) {
            return new RespVerbatimString();
        }
        if (line.length() < (length + 2)) {
            throw new Resp3ParseException("Unterminated end of verbatim string", true);
        }
        content = line.substring(length);
        var type = content.substring(0,3);
        content = content.substring(4);
        if(line.charAtAndIncrement()!='\r'){
            throw new Resp3ParseException("Invalid bulk string format");
        }

        if(line.charAtAndIncrement()!='\n'){
            throw new Resp3ParseException("Invalid bulk string format");
        }
        var res = new RespVerbatimString();
        res.setType(type);
        res.setMsg(content);
        return res;
    }

    private Map<Object,Object> parseMap(Resp3Input line) throws Resp3ParseException {
        int length = -1;
        try{
            length = parseInteger(line);
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid map length",ex.isMissingData());
        }
        Map<Object,Object> content = null;
        if(length==-1) {
            return content;
        }
        try {
            content = new HashMap<>();
            for (var i = 0; i < length; i++) {
                var key = parse(line);
                var value = parse(line);
                content.put(key,value);
            }
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid map format",ex.isMissingData());
        }
        return content;
    }

    private Set<Object> parseSet(Resp3Input line) throws Resp3ParseException {
        int length = -1;
        try{
            length = parseInteger(line);
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid set length",ex.isMissingData());
        }
        Set<Object> content = null;
        if(length==-1) {
            return content;
        }
        try {
            content = new HashSet<>();
            for (var i = 0; i < length; i++) {
                var key = parse(line);
                content.add(key);
            }
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid set format",ex.isMissingData());
        }
        return content;
    }

    public Object parse(Resp3Input line) throws Resp3ParseException {

        char prefix = line.charAtAndIncrement();
        switch (prefix) {
            case '+':
                return parseSimpleString(line);
            case '-':
                return parseSimpleError(line);
            case ':':
                return parseInteger(line);
            case '$':
                return parseBulkString(line);
            case '*':
                return parseArray(line);
            case '_':
                return parseNull(line);
            case '#':
                return parseBool(line);
            case ',':
                return parseDouble(line);
            case '(':
                return parseBigNumber(line);
            case '!':
                return parseBulkError(line);
            case '=':
                return parseVerbatimString(line);
            case '%':
                return parseMap(line);
            case '~':
                return parseSet(line);
            case '>':
                return parsePush(line);
            default:
                throw new Resp3ParseException("Unknown response type: " + prefix);
        }


    }

    private RespPush parsePush(Resp3Input line) throws Resp3ParseException {
        int length = -1;
        try{
            length = parseInteger(line);
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid push length",ex.isMissingData());
        }
        RespPush content = null;
        if(length==-1) {
            return content;
        }
        try {
            content = new RespPush();
            for (var i = 0; i < length; i++) {
                var result = parse(line);
                content.add(result);
            }
        }catch (Resp3ParseException ex){
            throw new Resp3ParseException("Invalid push format",ex.isMissingData());
        }
        return content;
    }
}
