package org.kendar.http.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.http.utils.converters.RequestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Response {
    private JsonNode responseText;
    private Map<String, List<String>> headers = new HashMap<>();
    private int statusCode = 200;

    private List<String> messages = new ArrayList<>();

    public JsonNode getResponseText() {
        return responseText;
    }

    public void setResponseText(JsonNode responseText) {
        this.responseText = responseText;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getFirstHeader(String s) {
        var result = getHeader(s);
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    public List<String> getHeader(String s) {
        if (this.headers == null) this.headers = new HashMap<>();
        return RequestUtils.getFromMapList(this.headers, s);
    }

    public void addHeader(String key, String value) {
        RequestUtils.addToMapList(headers, key, value);
    }

    public Response copy() {
        var r = new Response();
        r.headers = this.headers.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        r.responseText = this.responseText;
        r.statusCode = this.statusCode;

        return r;
    }

    public void removeHeader(String s) {
        for (var kvp : headers.keySet()) {
            if (s.equalsIgnoreCase(kvp)) {
                headers.remove(kvp);
                return;
            }
        }
    }
}
