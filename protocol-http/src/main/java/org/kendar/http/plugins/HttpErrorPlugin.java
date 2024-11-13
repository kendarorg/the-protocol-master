package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HttpErrorPlugin extends ProtocolPluginDescriptor<Request, Response> {

    private static final Logger log = LoggerFactory.getLogger(HttpErrorPlugin.class);
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
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        return this;
    }

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request request, Response response) {
        if (!isActive()) return false;
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

    @Override
    public Class<?> getSettingClass() {
        return HttpErrorPluginSettings.class;
    }

    @Override
    public void setSettings(PluginSettings plugin) {
        super.setSettings(plugin);
        var settings = (HttpErrorPluginSettings) plugin;
        this.errorCode = settings.getShowError();
        this.errorMessage = settings.getErrorMessage();
        this.percentage = ((double) settings.getErrorPercent()) / 100.0;
    }
}
