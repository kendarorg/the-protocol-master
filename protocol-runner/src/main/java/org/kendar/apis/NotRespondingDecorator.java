package org.kendar.apis;

import com.sun.net.httpserver.HttpExchange;
import org.kendar.plugins.base.BaseApiServerHandler;

import java.util.Map;

public class NotRespondingDecorator implements BaseApiServerHandler {
    private final BaseApiServerHandler handler;

    public NotRespondingDecorator(BaseApiServerHandler handler) {
        this.handler = handler;
    }

    @Override
    public void respond(HttpExchange exchange, Object toSend, int errorCode) {

    }

    @Override
    public boolean isPartialPath(String path, String api) {
        return handler.isPartialPath(path, api);
    }

    @Override
    public boolean isPath(String path, String api, Map<String, String> parameters) {
        return handler.isPath(path, api, parameters);
    }

    @Override
    public boolean isPath(String path, String api) {
        return handler.isPath(path, api);
    }
}
