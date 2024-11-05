package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.http.utils.filters.HttpFilterDescriptor;
import org.kendar.http.utils.filters.HttpPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ErrorFilter implements HttpFilterDescriptor {

    private static final Logger log = LoggerFactory.getLogger(ErrorFilter.class);
    private int errorCode;
    private String errorMessage;
    private double percentage;

    @Override
    public List<HttpPhase> getPhases() {
        return List.of(HttpPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "error-plugin";
    }

    @Override
    public void initialize(Map<String, Object> section) {
        this.errorCode = Integer.parseInt(section.get("errorCode").toString());
        this.errorMessage = section.get("errorMessage").toString();
        this.percentage = (double) Integer.parseInt(section.get("percentage").toString()) / 100;
    }

    @Override
    public boolean handle(HttpPhase phase, Request request, Response response) {
        if (Math.random() < percentage) {

            log.info("FAKE ERR " + request.getMethod() + " " + request.buildUrl());
            response.setStatusCode(errorCode);
            response.setResponseText(errorMessage);
            return true;
        }
        return false;
    }

    @Override
    public void terminate() {

    }
}
