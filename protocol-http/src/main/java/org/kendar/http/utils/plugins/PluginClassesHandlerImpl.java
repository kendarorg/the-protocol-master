package org.kendar.http.utils.plugins;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class PluginClassesHandlerImpl implements PluginClassesHandler {
    private final List<ProtocolPluginDescriptor> handlers;

    public PluginClassesHandlerImpl(List<ProtocolPluginDescriptor> handlers) {
        this.handlers = handlers;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request request, Response response, HttpClientConnectionManager connectionManager) throws InvocationTargetException, IllegalAccessException {
        for (var handler : handlers.stream().filter(h -> h.getPhases().contains(phase)).collect(Collectors.toList())) {
            var pfd = (ProtocolPluginDescriptor) handler;
            if (pfd.handle(pluginContext, phase, request, response)) {
                return true;
            }
        }
        return false;
    }
}
