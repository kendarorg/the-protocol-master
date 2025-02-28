package org.kendar.http.plugins;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.settings.HttpLatencyPluginSettings;
import org.kendar.plugins.BasicPercentPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.ChaosUtils;
import org.kendar.utils.JsonMapper;

import java.util.List;

@TpmService(tags = "http")
public class HttpLatencyPlugin extends BasicPercentPlugin<HttpLatencyPluginSettings> {

    private List<MatchingRecRep> target;

    public HttpLatencyPlugin(JsonMapper mapper) {
        super(mapper);
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

    @Override
    public Class<?> getSettingClass() {
        return HttpLatencyPluginSettings.class;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (shouldRun()) {
            if (SiteMatcherUtils.matchSite(in, target)) {
                HttpLatencyPluginSettings s = getSettings();
                ChaosUtils.randomWait(s.getMinMs(), s.getMaxMs());
            }
        }
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "latency-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }
}
