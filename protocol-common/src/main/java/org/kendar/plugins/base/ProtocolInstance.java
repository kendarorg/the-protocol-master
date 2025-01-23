package org.kendar.plugins.base;

import org.kendar.settings.ProtocolSettings;
import org.kendar.tcpserver.TcpServer;

import java.util.List;

public class ProtocolInstance {
    private final String protocolInstanceId;
    private final TcpServer server;
    private final List<ProtocolPluginDescriptor> plugins;
    private final ProtocolSettings settings;
    private final String protocol;

    public ProtocolInstance(String protocolInstanceId,
                            TcpServer server,
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

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public TcpServer getServer() {
        return server;
    }

    public List<ProtocolPluginDescriptor> getPlugins() {
        return plugins;
    }

    public ProtocolSettings getSettings() {
        return settings;
    }
}
