package org.kendar.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.buffers.BBuffer;

import java.util.Base64;

/**
 * Wrapper to have a single json serializer
 */
public class JsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public String serializePretty(Object target) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String serialize(Object target) {
        try {
            if (target instanceof BBuffer) {
                return mapper.writeValueAsString((((BBuffer) target).getAll()));
            } else {
                return mapper.writeValueAsString(target);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(Object serialized, Class<T> target) {
        try {
            if (serialized == null) return null;
            if (serialized instanceof String) return mapper.readValue((String) serialized, target);
            if (serialized instanceof JsonNode) return mapper.treeToValue((JsonNode) serialized, target);
            throw new RuntimeException("ERROR");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(Object serialized, TypeReference<T> target) {
        try {
            if (serialized == null) return null;
            if (serialized instanceof String) return mapper.readValue((String) serialized, target);
            if (serialized instanceof JsonNode) return mapper.treeToValue((JsonNode) serialized, target);
            throw new RuntimeException("ERROR");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public JsonNode toJsonNode(Object of) {
        try {
            if (of instanceof String) {
                try {
                    return mapper.readTree((String) of);
                } catch (JsonParseException e) {
                    return new TextNode((String) of);
                }
            }
            if (of instanceof JsonNode) {
                return (JsonNode) of;
            }
            if (of instanceof BBuffer) {
                return new BinaryNode(((BBuffer) of).getAll());
            }
            return mapper.readTree(mapper.writeValueAsString(of));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ContentData toGenericContent(byte[] input, String contentType) {
        var result = new ContentData();
        result.setBytes(input);
        if (contentType != null && !contentType.isEmpty()) {
            if (contentType.contains("text") ||
                    contentType.contains("xml")) {
                result.setChars(new TextNode(new String(input)));
                return result;
            }else if(contentType.contains("json")){
                try {
                    result.setChars(mapper.readTree(input));
                }catch (Exception ex){
                    result.setChars(new TextNode(new String(input)));
                }
                return result;
            }
        }
        var strData = new String(input);
        var doubleConversion = strData.getBytes();
        if (doubleConversion.length != input.length) {
            result.setBytes(input);
            return result;
        }
        for (int i = 0; i < input.length; i++) {
            if (doubleConversion[i] != input[i]) {
                result.setBytes(input);
                return result;
            }
        }
        result.setBytes(input);
        try{
            var pre = mapper.readTree(strData);
            result.setChars(pre);
        }catch (Exception ex){
            result.setChars(new TextNode(strData));
        }
        return result;
    }

    public byte[] fromGenericContent(ContentData content) {
        if(content.getBytes()==null||content.getBytes().length==0){
            if(content.getChars()==null){
                return new byte[]{};
            }else{
                return content.getChars().toString().getBytes();
            }
        }else{
            return content.getBytes();
        }
    }

    public String toHumanReadable(ContentData content){
        if(content.getBytes()==null||content.getBytes().length==0){
            if(content.getChars()==null){
                return "";
            }else{
                return content.getChars().toString();
            }
        }else{
            if(content.getChars()!=null){
                return "STR:"+content.getChars().toString()+"\n" +
                        "B64:"+Base64.getEncoder().encodeToString(content.getBytes());
            }
            return Base64.getEncoder().encodeToString(content.getBytes());
        }
    }
}
