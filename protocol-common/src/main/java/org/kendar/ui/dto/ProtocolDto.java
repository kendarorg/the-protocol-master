package org.kendar.ui.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtocolDto  extends BaseHtmxDto{
    private List<PluginDto> plugins = new ArrayList<PluginDto>();
    private String protocol;
    private String instanceId;
    private Map<String,Integer> openPorts = new HashMap<>();

    public Map<String, Integer> getOpenPorts() {
        return openPorts;
    }

    public void setOpenPorts(Map<String, Integer> openPorts) {
        this.openPorts = openPorts;
    }

    public List<PluginDto> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PluginDto> plugins) {
        this.plugins = plugins;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getSafeInstanceId() {
        return instanceId.replaceAll("-","");
    }
}
