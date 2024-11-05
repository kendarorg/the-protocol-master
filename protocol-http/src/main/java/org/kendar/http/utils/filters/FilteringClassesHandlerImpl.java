package org.kendar.http.utils.filters;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class FilteringClassesHandlerImpl implements FilteringClassesHandler {
    private final List<HttpFilterDescriptor> handlers;

    public FilteringClassesHandlerImpl(List<HttpFilterDescriptor> handlers) {
        this.handlers = handlers;
    }

    @Override
    public boolean handle(HttpPhase phase, Request request, Response response, HttpClientConnectionManager connectionManager) throws InvocationTargetException, IllegalAccessException {
        for (var handler : handlers.stream().filter(h -> h.getPhases().contains(phase)).collect(Collectors.toList())) {
            if (handler.handle(phase, request, response)) {
                return true;
            }
        }
        return false;
    }
}
