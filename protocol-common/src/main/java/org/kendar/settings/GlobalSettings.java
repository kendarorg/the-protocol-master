package org.kendar.settings;

import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class GlobalSettings {
    protected static final JsonMapper mapper = new JsonMapper();
    private final Map<String, Object> services = new HashMap<>();
    private final String logType = "file";
    private String pluginsDir = "plugins";
    private String logLevel = "INFO";
    private String dataDir = "data";
    private int apiPort = 0;
    private Map<String, Object> protocols = new HashMap<>();
    private Map<String, Object> plugins = new HashMap<>();
    private boolean unattended = false;

    public int getApiPort() {
        return apiPort;
    }

    public void setApiPort(int apiPort) {
        this.apiPort = apiPort;
    }


    public ProtocolSettings getProtocolForKey(String protocol) {
        if (!protocols.containsKey(protocol)) {
            return null;
        }
        if (ProtocolSettings.class.isAssignableFrom(protocols.get(protocol).getClass())) {
            return (ProtocolSettings) protocols.get(protocol);
        }
        return mapper.deserialize(mapper.serialize(protocols.get(protocol)), ProtocolSettings.class);
    }

    public ProtocolSettings getProtocol(String protocol, Class<?> clazz) {
        if (!protocols.containsKey(protocol)) {
            return null;
        }
        var protocolData = protocols.get(protocol);
        if (clazz.isAssignableFrom(protocolData.getClass())) {
            return (ProtocolSettings) protocolData;
        } else {
            var deserialized = mapper.deserialize(mapper.serialize(protocolData), clazz);
            protocols.put(protocol, deserialized);
            return (ProtocolSettings) deserialized;
        }
    }

    public PluginSettings getPlugin(String plugin, Class<?> clazz) {
        if (!plugins.containsKey(plugin)) {
            return null;
        }
        var pluginsData = plugins.get(plugin);
        if (clazz.isAssignableFrom(pluginsData.getClass())) {
            return (PluginSettings) pluginsData;
        } else {
            var deserialized = mapper.deserialize(mapper.serialize(pluginsData), clazz);
            plugins.put(plugin, deserialized);
            return (PluginSettings) deserialized;
        }
    }

    public Map<String, Object> getProtocols() {
        return protocols;
    }

    public void setProtocols(Map<String, Object> protocols) {
        this.protocols = protocols;
    }

    public Map<String, Object> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, Object> protocols) {
        this.plugins = protocols;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public boolean isUnattended() {
        return unattended;
    }

    public void setUnattended(boolean unattended) {
        this.unattended = unattended;
    }
}
