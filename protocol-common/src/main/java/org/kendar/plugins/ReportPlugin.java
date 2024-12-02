package org.kendar.plugins;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.settings.PluginSettings;

import java.util.List;

public abstract class ReportPlugin <J,K,W extends PluginSettings> extends ProtocolPluginDescriptorBase<J,K, W> {
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL,ProtocolPhase.ASYNC_RESPONSE);
    }

    //public abstract boolean handle(PluginContext pluginContext, ProtocolPhase phase, J in, K out);

    @Override
    public String getId() {
        return "report-plugin";
    }
}
