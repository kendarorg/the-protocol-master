package org.kendar.http.plugins;

import org.kendar.filters.PluginDescriptor;
import org.kendar.filters.ProtocolPhase;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ErrorPlugin extends ProtocolPluginDescriptor<Request, Response> {

    private static final Logger log = LoggerFactory.getLogger(ErrorPlugin.class);
    private boolean active;
    private int errorCode;
    private String errorMessage;
    private double percentage;

    public ErrorPlugin(Map<String, Object> section) {
        try {
            active = Integer.parseInt(section.get("error.errorCode").toString()) > 0 &&
                    Integer.parseInt(section.get("error.percentage").toString()) > 0;
        }catch (Exception e){
            active=false;
        }
    }

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
    public PluginDescriptor initialize(Map<String, Object> section, Map<String, Object> global) {
        this.errorCode = Integer.parseInt(section.get("error.errorCode").toString());
        this.errorMessage = section.get("error.errorMessage").toString();
        this.percentage = (double) Integer.parseInt(section.get("error.percentage").toString()) / 100;
        return this;
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

    public boolean isActive() {
        return active;
    }
}
