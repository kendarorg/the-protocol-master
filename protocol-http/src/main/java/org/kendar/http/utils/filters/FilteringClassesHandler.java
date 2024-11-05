package org.kendar.http.utils.filters;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;

import java.lang.reflect.InvocationTargetException;

public interface FilteringClassesHandler {
    boolean handle(
            HttpPhase filterType,
            Request request,
            Response response,
            HttpClientConnectionManager connectionManager)
            throws InvocationTargetException, IllegalAccessException;
}
