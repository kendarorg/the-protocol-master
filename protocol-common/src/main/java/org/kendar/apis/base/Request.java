package org.kendar.apis.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.apis.converters.MultipartPart;
import org.kendar.apis.converters.RequestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Request {
    private static final AtomicLong counter = new AtomicLong(0);

    private long id = counter.incrementAndGet();
    private long ms = Calendar.getInstance().getTimeInMillis();
    private String method;
    private JsonNode requestText;
    private Map<String, List<String>> headers;
    private String protocol;
    private boolean soapRequest;
    private String basicPassword;
    private String basicUsername;
    private List<MultipartPart> multipartData = new ArrayList<>();
    private boolean staticRequest;
    private String host;
    private String path;
    private Map<String, String> postParameters = new HashMap<>();
    private int port;
    private Map<String, String> query = new HashMap<>();
    private String remoteHost;
    private Map<String, String> pathParameters = new HashMap<>();
    private Request original;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonNode getRequestText() {
        return requestText;
    }

    public void setRequestText(JsonNode requestText) {
        this.requestText = requestText;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isSoapRequest() {
        return soapRequest;
    }

    public void setSoapRequest(boolean soapRequest) {
        this.soapRequest = soapRequest;
    }

    public String getBasicPassword() {
        return basicPassword;
    }

    public void setBasicPassword(String basicPassword) {
        this.basicPassword = basicPassword;
    }

    public String getBasicUsername() {
        return basicUsername;
    }

    public void setBasicUsername(String basicUsername) {
        this.basicUsername = basicUsername;
    }

    public List<MultipartPart> getMultipartData() {
        return multipartData;
    }

    public void setMultipartData(List<MultipartPart> multipartData) {
        this.multipartData = multipartData;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getPostParameters() {
        return postParameters;
    }

    public void setPostParameters(Map<String, String> postParameters) {
        this.postParameters = postParameters;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }

    public List<String> getHeader(String id) {

        return RequestUtils.getFromMapList(this.headers, id);
    }

    public String getFirstHeader(String id, String defaultValue) {

        var result = getHeader(id);
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return defaultValue;
    }

    public String getFirstHeader(String id) {

        var result = getHeader(id);
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    public void addPathParameter(String key, String value) {
        if (this.pathParameters == null) this.pathParameters = new HashMap<>();
        RequestUtils.addToMap(this.pathParameters, key, value);
    }

    public String getPathParameter(String id) {
        return RequestUtils.getFromMap(this.pathParameters, id);
    }

    public String getRequestParameter(String key) {
        var result = getPostParameter(key);
        if (result == null) {
            result = getQuery(key);
        }
        if (result == null) {
            result = getFirstHeader(key);
        }
        if (result == null) {
            result = getPathParameter(key);
        }
        return result;
    }

    public void addHeader(String key, String value) {
        if (this.headers == null) this.headers = new HashMap<>();
        RequestUtils.addToMapList(this.headers, key, value);
    }

    public void addQuery(String key, String value) {

        if (this.query == null) this.query = new HashMap<>();
        RequestUtils.addToMap(this.query, key, value);
    }

    public String getQuery(String id) {
        return RequestUtils.getFromMap(this.query, id);
    }

    public String getPostParameter(String id) {
        return RequestUtils.getFromMap(this.postParameters, id);
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String extractRemoteHostName() {
        try {
            return InetAddress.getByName(remoteHost).getHostName();
        } catch (UnknownHostException e) {
            return remoteHost;
        }
    }

    public long getMs() {
        return ms;
    }

    public void setMs(long ms) {
        this.ms = ms;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public Request copy() {
        var r = new Request();
        r.id = this.id;
        r.pathParameters = this.pathParameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        r.ms = this.ms;
        r.remoteHost = this.remoteHost;
        r.path = this.path;
        r.basicPassword = this.basicPassword;
        r.basicUsername = this.basicUsername;
        if (headers != null) {
            r.headers = this.headers.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        r.host = this.host;
        r.method = this.method;
        if (multipartData != null) {
            r.multipartData = this.multipartData.stream().map(MultipartPart::copy).collect(Collectors.toList());
        }
        r.port = this.port;
        if (postParameters != null) {
            r.postParameters = this.postParameters.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        r.protocol = this.protocol;
        if (query != null) {
            r.query = this.query.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        r.requestText = this.requestText;
        r.soapRequest = this.soapRequest;
        return r;
    }

    public void addOriginal(Request oriSource) {
        this.original = oriSource;
    }

    public Request retrieveOriginal() {
        if (original != null) return original;
        return this;
    }

    public String findCookie(String value) {
        var cookies = this.getFirstHeader("Cookie");
        if (cookies == null) return null;
        var splittedCookies = cookies.split(";");
        for (var cookie : splittedCookies) {
            var cookieData = cookie.trim().split("=");
            if (value.equalsIgnoreCase(cookieData[0].trim())) {
                return cookieData[1].trim();
            }
        }
        return null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String buildUrl() {
        var query = getQuery().entrySet().stream().
                sorted(Comparator.comparing(Map.Entry<String, String>::getKey)).
                map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
        if (!query.isEmpty()) query = "?" + query;
        if (getPort() > 0) {
            return getProtocol() + "://" + getHost() + ":" + getPort() +
                    getPath() + query;
        } else {

            return getProtocol() + "://" + getHost() +
                    getPath() + query;
        }
    }

    public void removeHeader(String s) {
        for (var kvp : headers.keySet()) {
            if (s.equalsIgnoreCase(kvp)) {
                headers.remove(kvp);
                return;
            }
        }
    }

    public int getSize() {
        if (this.getRequestText() instanceof BinaryNode) {
            return this.getRequestText().size();
        } else if (this.getRequestText() instanceof TextNode) {
            return this.getRequestText().size();
        }
        return 0;
    }
}
