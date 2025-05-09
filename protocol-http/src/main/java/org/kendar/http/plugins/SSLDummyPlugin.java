package org.kendar.http.plugins;

import org.kendar.di.annotations.TpmPostConstruct;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.HttpProtocolSettings;
import org.kendar.http.plugins.apis.SSLApiHandler;
import org.kendar.http.plugins.settings.SSLDummyPluginSettings;
import org.kendar.plugins.base.*;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.settings.ProtocolSettings;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

import java.util.List;

@TpmService(tags = "http")
public class SSLDummyPlugin extends ProtocolPluginDescriptorBase<SSLDummyPluginSettings> implements AlwaysActivePlugin {
    private final MultiTemplateEngine resolversFactory;
    private HttpProtocolSettings protocolSettings;

    public SSLDummyPlugin(JsonMapper mapper, MultiTemplateEngine resolversFactory) {
        super(mapper);
        this.resolversFactory = resolversFactory;
    }

    @TpmPostConstruct
    public void postConstruct() {
        setActive(true);
    }

    @Override
    public Class<?> getSettingClass() {
        return SSLDummyPluginSettings.class;
    }

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

    @Override
    protected List<ProtocolPluginApiHandler> buildApiHandler() {
        return List.of(new SSLApiHandler(this, getId(), getInstanceId(), protocolSettings, resolversFactory));
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
