package org.kendar.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;

public class ContentData {
    private byte[] bytes;
    private JsonNode chars;

    @Override
    public String toString() {
        return "ContentData{" +
                "bytes=" + Arrays.toString(bytes) +
                ", chars=" + chars +
                '}';
    }

    public JsonNode getChars() {
        return chars;
    }

    public void setChars(JsonNode chars) {
        this.chars = chars;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

}
