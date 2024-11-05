package org.kendar.http.utils.callexternal;

import org.kendar.http.utils.Event;
import org.kendar.http.utils.Request;

public class ExecuteLocalRequest implements Event {
    private Request request;

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
