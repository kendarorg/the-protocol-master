package org.kendar.http.utils.filters;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.filters.PluginDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.proxy.FilterContext;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class FilteringClassesHandlerImpl implements FilteringClassesHandler {
    private final List<PluginDescriptor> handlers;

    public FilteringClassesHandlerImpl(List<PluginDescriptor> handlers) {
        this.handlers = handlers;
    }

    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, Request request, Response response, HttpClientConnectionManager connectionManager) throws InvocationTargetException, IllegalAccessException {
        for (var handler : handlers.stream().filter(h -> h.getPhases().contains(phase)).collect(Collectors.toList())) {
            var pfd = (ProtocolPluginDescriptor) handler;
            if (pfd.handle(filterContext, phase, request, response)) {
                return true;
            }
        }
        return false;
    }
}
