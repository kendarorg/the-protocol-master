package org.kendar.redis.parser;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

import java.io.BufferedReader;
import java.io.IOException;


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


    private String parseSimpleString(String line) {
        if(line.endsWith("\r\n")){
            line = line.substring(0, line.length()-2);
        }
        return line;
    }
    private RespError parseSimpleError(String line) {
        if(line.endsWith("\r\n")){
            line = line.substring(0, line.length()-2);
        }
        var spl = line.split("\\s");
        var result = new RespError();
        if(isUpper(spl[0])){
            result.setType(spl[0]);
            result.setMsg(line.substring(spl[0].length()+1));
        }else{
            result.setMsg(line);
        }
        return result;
    }

    private int parseInteger(String line) throws IOException {
        try {
            if (line.endsWith("\r\n")) {
                line = line.substring(0, line.length() - 2);
            }
            var positive = true;
            if (line.startsWith("+")) {
                line = line.substring(1);
            } else if (line.startsWith("-")) {
                line = line.substring(1);
                positive = false;
            }
            var result = Integer.parseInt(line);
            if (!positive) {
                result = -result;
            }
            return result;
        }catch (Exception ex){
            throw new IOException("Invalid integer");
        }
    }
    private String parseBulkString(String line) throws IOException {
        if(line.endsWith("\r\n")){
            line = line.substring(0, line.length()-2);
        }
        var lengthStr = "";
        var expectedLength = 0;
        while(line.charAt(0)<='9' && line.charAt(0)>='0'){
            lengthStr+=line.charAt(0);
            line=line.substring(1);
        }
        if(line.startsWith("\r\n")){
            line = line.substring(2);
        }else{
            throw new IOException("Invalid bulk string");
        }
        try{
            expectedLength = Integer.parseInt(lengthStr);
        }catch (Exception ex) {
            throw new IOException("Invalid bulk string length");
        }
        if(expectedLength!=line.length()){
            throw new IOException(
                    String.format("Not matchin bulk string length expected:%d found:%d",expectedLength,line.length()));
        }
        return line;
    }

    public Object parse(String line) throws IOException {

        char prefix = line.charAt(0);
        switch (prefix) {
            case '+':
                return parseSimpleString(line.substring(1));
            case '-':
                return parseSimpleError(line.substring(1));
            case ':':
                return parseInteger(line.substring(1));
            case '$':
                return parseBulkString(line.substring(1));
            default:
                throw new IOException("Unknown response type: " + prefix);
        }
    }
}
