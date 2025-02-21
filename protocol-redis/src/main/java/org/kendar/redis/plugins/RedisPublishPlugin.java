package org.kendar.redis.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.redis.api.RedisPublishPluginApis;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.utils.JsonMapper;

import java.util.List;

@TpmService(tags = "redis")
public class RedisPublishPlugin extends ProtocolPluginDescriptorBase<PluginSettings> {
    public RedisPublishPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        return super.initialize(global, protocol, pluginSetting);
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getProtocol() {
        return "redis";
    }

    @Override
    public String getId() {
        return "publish-plugin";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Object in, Object out) {
        return false;
    }

    @Override
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new RedisPublishPluginApis(this, getId(), getInstanceId()));
    }
}
