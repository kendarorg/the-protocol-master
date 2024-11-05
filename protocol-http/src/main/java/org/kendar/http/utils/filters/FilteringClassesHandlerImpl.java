package org.kendar.http.utils.filters;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.filters.FilterDescriptor;
import org.kendar.filters.ProtocolFilterDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class FilteringClassesHandlerImpl implements FilteringClassesHandler {
    private final List<FilterDescriptor> handlers;

    public FilteringClassesHandlerImpl(List<FilterDescriptor> handlers) {
        this.handlers = handlers;
    }

    @Override
    public boolean handle(ProtocolPhase phase, Request request, Response response, HttpClientConnectionManager connectionManager) throws InvocationTargetException, IllegalAccessException {
        for (var handler : handlers.stream().filter(h -> h.getPhases().contains(phase)).collect(Collectors.toList())) {
            var pfd = (ProtocolFilterDescriptor)handler;
            if (pfd.handle(phase, request, response)) {
                return true;
            }
        }
        return false;
    }
}
