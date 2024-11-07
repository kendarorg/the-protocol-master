package org.kendar.filters;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class HttpFilter extends ProtocolPluginDescriptor<Request, Response> implements AlwaysActivePlugin {
    @Override
    public boolean handle(ProtocolPhase phase, Request in, Response out) {
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "http-filter";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public PluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol) {
        return null;
    }


    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return HttpFilterSettings.class;
    }

    @Override
    public void setSettings(PluginSettings plugin) {

    }
}
