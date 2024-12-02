package org.kendar.sample.plugins;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class HttpFilter extends ProtocolPluginDescriptorBase<Request, Response, HttpFilterSettings> implements AlwaysActivePlugin {
    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        return false;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "sample-http";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting);
        return this;
    }


}
