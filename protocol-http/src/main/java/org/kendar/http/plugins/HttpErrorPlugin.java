package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@TpmService(tags = "http")
public class HttpErrorPlugin extends ProtocolPluginDescriptorBase<HttpErrorPluginSettings> {

    private static final Logger log = LoggerFactory.getLogger(HttpErrorPlugin.class);

    private List<MatchingRecRep> errorSites;

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
        if (!isActive()) return false;
        if (SiteMatcherUtils.matchSite(request, errorSites)) {
            var pc = ((double) getSettings().getErrorPercent()) / 100.0;
            if (Math.random() < pc) {

                log.info("Faking ERROR {}", request.buildUrl());
                response.setStatusCode(getSettings().getShowError());
                response.setResponseText(new TextNode(getSettings().getErrorMessage()));
                return true;
            }
        }

        return false;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        errorSites = SiteMatcherUtils.setupSites(getSettings().getErrorSites());
        return this;
    }
}
