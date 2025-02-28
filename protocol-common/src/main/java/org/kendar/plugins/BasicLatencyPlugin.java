package org.kendar.plugins;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.ChaosUtils;
import org.kendar.utils.JsonMapper;

import java.util.List;

public abstract class BasicLatencyPlugin<W extends LatencyPluginSettings> extends BasicPercentPlugin<W> {
    public BasicLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        if (shouldRun() && in != null) {
            ChaosUtils.randomWait(getSettings().getMinMs(), getSettings().getMaxMs());
        }
        return false;
    }


    @Override
    public String getId() {
        return "latency-plugin";
    }

    @Override
    public Class<?> getSettingClass() {
        return LatencyPluginSettings.class;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }
}
