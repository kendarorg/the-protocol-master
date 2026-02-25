package org.kendar.plugins.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.kendar.settings.ProtocolSettings;
import org.kendar.tcpserver.Server;

import java.util.List;

public class ProtocolInstance {
    @JsonIgnore
    private final String protocolInstanceId;
    @JsonIgnore
    private final Server server;
    private final List<ProtocolPluginDescriptor> plugins;
    private final ProtocolSettings settings;
    private final String protocol;

    public ProtocolInstance(String protocolInstanceId,
                            Server server,
                            List<ProtocolPluginDescriptor> plugins,
                            ProtocolSettings settings) {

        this.protocolInstanceId = protocolInstanceId;
        this.server = server;
        this.plugins = plugins;
        this.settings = settings;
        this.protocol = settings.getProtocol();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getInstanceId() {
        return protocolInstanceId;
    }

    public Server getServer() {
        return server;
    }

    public List<ProtocolPluginDescriptor> getPlugins() {
        return plugins;
    }

    public ProtocolSettings getSettings() {
        return settings;
    }
}
