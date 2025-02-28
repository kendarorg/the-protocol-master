package org.kendar.plugins;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;

import java.util.List;

public abstract class BasicReportPlugin<W extends PluginSettings> extends ProtocolPluginDescriptorBase<W> {
    public BasicReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL, ProtocolPhase.ASYNC_RESPONSE);
    }

    @Override
    public String getId() {
        return "report-plugin";
    }
}
