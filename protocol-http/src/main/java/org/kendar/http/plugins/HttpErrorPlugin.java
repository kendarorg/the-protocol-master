package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.commons.MatchingRecRep;
import org.kendar.http.plugins.commons.SiteMatcherUtils;
import org.kendar.http.plugins.settings.HttpErrorPluginSettings;
import org.kendar.plugins.BasicPercentPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@TpmService(tags = "http")
public class HttpErrorPlugin extends BasicPercentPlugin<HttpErrorPluginSettings> {

    private static final Logger log = LoggerFactory.getLogger(HttpErrorPlugin.class);

    private List<MatchingRecRep> target;

    public HttpErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpErrorPluginSettings.class;
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


    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request request, Response response) {
        if (!shouldRun()) return false;
        if (SiteMatcherUtils.matchSite(request, target)) {
            log.info("Faking ERROR {}", request.buildUrl());
            response.setStatusCode(getSettings().getShowError());
            response.setResponseText(new TextNode(getSettings().getErrorMessage()));
            return true;
        }

        return false;
    }

    @Override
    protected boolean handleSettingsChanged() {
        if (getSettings() == null) return false;
        target = SiteMatcherUtils.setupSites(getSettings().getTarget());
        return true;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        handleSettingsChanged();
        return this;
    }
}
