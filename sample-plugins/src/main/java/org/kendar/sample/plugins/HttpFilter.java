package org.kendar.sample.plugins;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.pf4j.Extension;

import java.util.List;

@Extension
public class HttpFilter extends ProtocolPluginDescriptorBase<HttpFilterSettings> implements AlwaysActivePlugin {
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


}
