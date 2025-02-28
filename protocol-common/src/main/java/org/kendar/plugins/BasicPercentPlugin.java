package org.kendar.plugins;

import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.plugins.settings.BasicPercentPluginSettings;
import org.kendar.utils.ChaosUtils;
import org.kendar.utils.JsonMapper;

public abstract class BasicPercentPlugin<W extends BasicPercentPluginSettings> extends ProtocolPluginDescriptorBase<W> {
    public BasicPercentPlugin(JsonMapper mapper) {
        super(mapper);
    }

    protected boolean shouldRun() {
        return isActive() && ChaosUtils.randomAction(getSettings().getPercentAction());
    }
}
