package org.kendar.http.utils.filters;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Map;

public interface HttpFilterDescriptor extends ExtensionPoint {
    List<HttpPhase> getPhases();

    String getId();

    void initialize(Map<String, Object> section);

    /**
     * @param phase
     * @param request
     * @param response
     * @return true when is blocking
     */
    boolean handle(HttpPhase phase, Request request, Response response);

    void terminate();
}
