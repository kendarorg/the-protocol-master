package org.kendar.http.plugins;

import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ErrorPlugin extends ProtocolPluginDescriptor<Request, Response> {

    private static final Logger log = LoggerFactory.getLogger(ErrorPlugin.class);
    private int errorCode;
    private String errorMessage;
    private double percentage;

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "error-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public void initialize(Map<String, Object> section) {
        this.errorCode = Integer.parseInt(section.get("errorCode").toString());
        this.errorMessage = section.get("errorMessage").toString();
        this.percentage = (double) Integer.parseInt(section.get("percentage").toString()) / 100;
    }

    @Override
    public boolean handle(ProtocolPhase phase, Request request, Response response) {
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
