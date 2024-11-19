package org.kendar.apis;

import org.kendar.command.CommonRunner;
import org.kendar.plugins.PluginDescriptor;
import org.kendar.settings.ProtocolSettings;

import java.util.List;

public class ProtocolInstance {
    private final String protocolInstanceId;
    private final CommonRunner protocolManager;
    private final List<PluginDescriptor> plugins;
    private final ProtocolSettings settings;

    public ProtocolInstance(String protocolInstanceId, CommonRunner protocolManager, List<PluginDescriptor> plugins, ProtocolSettings settings) {

        this.protocolInstanceId = protocolInstanceId;
        this.protocolManager = protocolManager;
        this.plugins = plugins;
        this.settings = settings;
    }

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public CommonRunner getProtocolManager() {
        return protocolManager;
    }

    public List<PluginDescriptor> getPlugins() {
        return plugins;
    }

    public ProtocolSettings getSettings() {
        return settings;
    }
}
