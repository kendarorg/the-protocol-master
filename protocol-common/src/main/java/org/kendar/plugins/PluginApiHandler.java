package org.kendar.plugins;

import com.sun.net.httpserver.HttpExchange;

public interface PluginApiHandler {
    boolean handle(HttpExchange exchange, String pathPart);
    String getId();
    String getProtocolInstanceId();
}
