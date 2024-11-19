package org.kendar.http.utils.plugins;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.proxy.PluginContext;

import java.lang.reflect.InvocationTargetException;

public interface PluginClassesHandler {
    boolean handle(
            PluginContext pluginContext, ProtocolPhase filterType,
            Request request,
            Response response,
            HttpClientConnectionManager connectionManager)
            throws InvocationTargetException, IllegalAccessException;
}
