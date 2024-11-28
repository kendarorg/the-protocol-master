package org.kendar.settings;

import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class GlobalSettings {
    protected static final JsonMapper mapper = new JsonMapper();
    private final Map<String, Object> services = new HashMap<>();
    private String pluginsDir = "plugins";
    private String logLevel = "ERROR";
    private String dataDir = "data";
    private String logType = "file";
    private int apiPort = 0;
    private Map<String, Object> protocols = new HashMap<>();
    private boolean unattended =false;

    public int getApiPort() {
        return apiPort;
    }

    public void setApiPort(int apiPort) {
        this.apiPort = apiPort;
    }

    public void putService(String key, Object value) {
        services.put(key, value);
    }

    public Object getService(String key) {
        return services.get(key);
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public ProtocolSettings getProtocolForKey(String protocol) {
        if (!protocols.containsKey(protocol)) {
            return null;
        }
        return mapper.deserialize(mapper.serialize(protocols.get(protocol)), ProtocolSettings.class);
    }

    public ProtocolSettings getProtocol(String protocol, Class<?> clazz) {
        if (!protocols.containsKey(protocol)) {
            return null;
        }
        var protocolData = protocols.get(protocol);
        return (ProtocolSettings) mapper.deserialize(mapper.serialize(protocolData), clazz);
    }

    public Map<String, Object> getProtocols() {
        return protocols;
    }

    public void setProtocols(Map<String, Object> protocols) {
        this.protocols = protocols;
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


    public void setUnattended(boolean unattended) {
        this.unattended = unattended;
    }

    public boolean isUnattended() {
        return unattended;
    }
}
