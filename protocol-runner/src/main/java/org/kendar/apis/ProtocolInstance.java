package org.kendar.apis;

import org.kendar.command.CommonRunner;
import org.kendar.filters.PluginDescriptor;
import org.kendar.settings.ProtocolSettings;

import java.util.List;

public class ProtocolInstance {
    private final String protocolInstanceId;
    private final CommonRunner protocolManager;
    private final List<PluginDescriptor> filters;
    private final ProtocolSettings settings;

    public ProtocolInstance(String protocolInstanceId, CommonRunner protocolManager, List<PluginDescriptor> filters, ProtocolSettings settings) {

        this.protocolInstanceId = protocolInstanceId;
        this.protocolManager = protocolManager;
        this.filters = filters;
        this.settings = settings;
    }

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public CommonRunner getProtocolManager() {
        return protocolManager;
    }

    public List<PluginDescriptor> getFilters() {
        return filters;
    }

    public ProtocolSettings getSettings() {
        return settings;
    }
}
