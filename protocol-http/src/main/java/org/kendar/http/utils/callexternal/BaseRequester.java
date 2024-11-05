package org.kendar.http.utils.callexternal;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;

public interface BaseRequester {
    void callSite(Request request, Response response)
            throws Exception;
}