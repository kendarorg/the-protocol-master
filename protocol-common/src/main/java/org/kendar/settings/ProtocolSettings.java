package org.kendar.settings;

import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class ProtocolSettings {
    private static JsonMapper mapper = new JsonMapper();
    private String protocol;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public PluginSettings getPlugin(String protocol, Class<?> clazz){
        if(!plugins.containsKey(protocol)){
            return null;
        }
        var protocolData = plugins.get(protocol);
        return (PluginSettings)mapper.deserialize(mapper.serialize(protocolData),clazz);
    }
    public Map<String, Object> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, Object> plugins) {
        this.plugins = plugins;
    }

    private Map<String,Object> plugins = new HashMap<String,Object>();

    public Map<String,PluginSettings> getSimplePlugins() {
        var result = new HashMap<String,PluginSettings>();
        for(var plugin : plugins.entrySet()){
            result.put(plugin.getKey(),mapper.deserialize(mapper.serialize(plugin.getValue()),PluginSettings.class));
        }
        return result;
    }

}
