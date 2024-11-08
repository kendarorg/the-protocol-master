package org.kendar.http.utils.filters;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.filters.ProtocolPhase;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.proxy.FilterContext;

import java.lang.reflect.InvocationTargetException;

public interface FilteringClassesHandler {
    boolean handle(
            FilterContext filterContext, ProtocolPhase filterType,
            Request request,
            Response response,
            HttpClientConnectionManager connectionManager)
            throws InvocationTargetException, IllegalAccessException;
}
