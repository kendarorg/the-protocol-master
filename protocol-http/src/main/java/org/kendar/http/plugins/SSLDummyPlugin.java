package org.kendar.http.plugins;

import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.plugins.base.*;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;

import java.util.List;

public class SSLDummyPlugin extends ProtocolPluginDescriptorBase<String, String, SSLDummyPluginSettings> implements AlwaysActivePlugin {
    private HttpProtocolSettings protocolSettings;

    @Override
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, String in, String out) {
        return false;
    }

    @Override
    public ProtocolPluginDescriptor initialize(GlobalSettings global, ProtocolSettings protocol, PluginSettings pluginSetting) {
        super.initialize(global, protocol, pluginSetting == null ? new SSLDummyPluginSettings() : pluginSetting);
        this.protocolSettings = (HttpProtocolSettings) protocol;
        return this;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    protected ProtocolPluginApiHandler buildApiHandler() {
        return new SSLApiHandler(this, getId(), getInstanceId(), protocolSettings);
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of();
    }

    @Override
    public String getId() {
        return "ssl-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }
}
