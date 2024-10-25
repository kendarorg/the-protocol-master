package org.kendar.http;

import org.kendar.http.data.Request;
import org.kendar.http.data.Response;

public class RoundTrip {
    private Request request;
    private Response response;

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public RoundTrip(Request request, Response response) {
        this.request = request;
        this.response = response;
    }
}
