package org.kendar.plugins;

import com.sun.net.httpserver.HttpExchange;

public interface PluginApiHandler {
    boolean handle(BaseApiServerHandler apiServerHandler, HttpExchange exchange, String pathPart);

    String getId();

    String getProtocolInstanceId();
}
