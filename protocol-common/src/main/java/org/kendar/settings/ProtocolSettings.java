package org.kendar.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class ProtocolSettings {
    protected static final JsonMapper mapper = new JsonMapper();
    private String protocol;
    @JsonIgnore
    private String protocolInstanceId;
    private Map<String, Object> plugins = new HashMap<>();

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public void setProtocolInstanceId(String protocolInstanceId) {
        this.protocolInstanceId = protocolInstanceId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public PluginSettings getPlugin(String pluginId, Class<?> clazz) {
        if (!plugins.containsKey(pluginId)) {
            return null;
        }
        var protocolData = plugins.get(pluginId);
        return (PluginSettings) mapper.deserialize(mapper.serialize(protocolData), clazz);
    }

    public Map<String, Object> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, Object> plugins) {
        this.plugins = plugins;
    }

    public Map<String, PluginSettings> getSimplePlugins() {
        var result = new HashMap<String, PluginSettings>();
        for (var plugin : plugins.entrySet()) {
            result.put(plugin.getKey(), mapper.deserialize(mapper.serialize(plugin.getValue()), PluginSettings.class));
        }
        return result;
    }

}
