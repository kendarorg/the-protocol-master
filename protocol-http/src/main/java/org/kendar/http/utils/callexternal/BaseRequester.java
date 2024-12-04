package org.kendar.http.utils.callexternal;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;

public interface BaseRequester {
    void callSite(Request request, Response response)
            throws Exception;
}