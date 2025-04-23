package org.kendar.plugins;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.BasicPercentPluginSettings;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.ChaosUtils;
import org.kendar.utils.JsonMapper;

import java.util.List;

public abstract class BasicNetworkErrorPlugin<W extends BasicPercentPluginSettings> extends BasicPercentPlugin<W> {
    public BasicNetworkErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, byte[] in, Object out) {
        if (shouldRun() && in != null && in.length > 0) {
            var modified = false;
            for (var i = 0; i < in.length; i++) {
                if (ChaosUtils.randomAction(5))
                {
                    modified = true;
                    in[i] = (byte) ChaosUtils.randomBetween(0, 256);
                }
            }
            if (!modified) {
                in[0] = (byte) ChaosUtils.randomBetween(0, 256);
            }
        }
        return false;
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicPercentPluginSettings.class;
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_SOCKET_WRITE);
    }

    @Override
    public String getId() {
        return "network-error-plugin";
    }
}
