package org.kendar.http;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.http.utils.plugins.PluginClassesHandler;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.PluginHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginClassesHandlerImpl implements PluginClassesHandler {
    private final Map<ProtocolPhase, List<PluginHandler>> plugins;

    public PluginClassesHandlerImpl(List<ProtocolPluginDescriptor> inputPlugins) {
        this.plugins = new HashMap<>();
        for (var plugin : inputPlugins) {
            var handlers = PluginHandler.of(plugin);
            for (var phase : plugin.getPhases()) {
                if (!this.plugins.containsKey(phase)) {
                    this.plugins.put((ProtocolPhase) phase, new ArrayList<>());
                }
                this.plugins.get(phase).addAll(handlers);
            }
        }
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request request, Response response, HttpClientConnectionManager connectionManager) throws InvocationTargetException, IllegalAccessException {
        if (!this.plugins.containsKey(phase)) {
            return false;
        }
        for (var handler : this.plugins.get(phase)) {
            if (handler.handle(pluginContext, phase, request, response)) {
                return true;
            }
        }
        return false;
    }
}
