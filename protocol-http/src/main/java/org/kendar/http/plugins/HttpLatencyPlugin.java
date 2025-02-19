package org.kendar.http.plugins;

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
import org.kendar.utils.Sleeper;

import java.util.List;
import java.util.Random;

@TpmService(tags = "http")
public class HttpLatencyPlugin extends ProtocolPluginDescriptorBase<HttpLatencyPluginSettings> {

    private List<MatchingRecRep> latencySites;

    public HttpLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    protected boolean handleSettingsChanged(){
        if(getSettings()==null) return false;
        latencySites = SiteMatcherUtils.setupSites(getSettings().getLatencySites());
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
        if (isActive()) {
            if (SiteMatcherUtils.matchSite(in, latencySites)) {
                HttpLatencyPluginSettings s = getSettings();
                Random r = new Random();
                int waitMs = r.nextInt(s.getMaxMs() - s.getMinMs()) + s.getMinMs();
                if (waitMs > 0) {
                    Sleeper.sleep(waitMs);
                }
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
