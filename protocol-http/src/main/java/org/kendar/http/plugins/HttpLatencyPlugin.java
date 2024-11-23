package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.Sleeper;

import java.util.List;
import java.util.Random;

public class HttpLatencyPlugin extends ProtocolPluginDescriptor<Request, Response> {
    private HttpLatencyPluginSettings settings;

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if(isActive()){
            Random r = new Random();
            int waitMs = r.nextInt(settings.getMaxMs()- settings.getMinMs()) +settings.getMinMs();
            if(waitMs>0) {
                Sleeper.sleep(waitMs);
            }
        }
        return false;
    }

    @Override
    public PluginDescriptor setSettings(PluginSettings plugin) {
        setActive(plugin.isActive());
        settings = (HttpLatencyPluginSettings)plugin;
        return this;
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

    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return HttpLatencyPluginSettings.class;
    }
}
