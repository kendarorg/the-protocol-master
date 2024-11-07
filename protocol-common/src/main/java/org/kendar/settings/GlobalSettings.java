package org.kendar.settings;

import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class GlobalSettings {
    private static JsonMapper mapper = new JsonMapper();
    private String pluginsDir;
    private String logLevel;
    private String dataDir;
    private String logType;

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public ProtocolSettings getProtocolForKey(String protocol) {
        if(!protocols.containsKey(protocol)){
            return null;
        }
        return mapper.deserialize(mapper.serialize(protocols.get(protocol)),ProtocolSettings.class);
    }

    public ProtocolSettings getProtocol(String protocol, Class<?> clazz){
        if(!protocols.containsKey(protocol)){
            return null;
        }
        var protocolData = protocols.get(protocol);
        return (ProtocolSettings)mapper.deserialize(mapper.serialize(protocolData),clazz);
    }

    public Map<String, Object> getProtocols() {
        return protocols;
    }

    public void setProtocols(Map<String, Object> protocols) {
        this.protocols = protocols;
    }

    private Map<String,Object> protocols = new HashMap<String,Object>();

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


}
