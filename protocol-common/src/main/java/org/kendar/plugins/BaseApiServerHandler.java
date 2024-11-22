package org.kendar.plugins;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public interface BaseApiServerHandler {
    void respond(HttpExchange exchange, Object toSend, int errorCode);

    boolean isPartialPath(String path, String api);

    boolean isPath(String path, String api, Map<String, String> parameters);

    boolean isPath(String path, String api);
}
