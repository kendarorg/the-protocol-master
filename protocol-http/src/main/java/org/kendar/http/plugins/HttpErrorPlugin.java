package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.base.BaseProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HttpErrorPlugin extends BaseProtocolPluginDescriptor<Request, Response, HttpErrorPluginSettings> {

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
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request request, Response response) {
        if (!isActive()) return false;
        if (Math.random() < percentage) {

            log.info("Faking ERROR {} {}", request.getMethod(), request.buildUrl());
            response.setStatusCode(errorCode);
            response.setResponseText(new TextNode(errorMessage));
            return true;
        }
        return false;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        var settings = (HttpErrorPluginSettings) pluginSetting;
        this.errorCode = settings.getShowError();
        this.errorMessage = settings.getErrorMessage();
        this.percentage = ((double) settings.getErrorPercent()) / 100.0;
        return this;
    }
}
