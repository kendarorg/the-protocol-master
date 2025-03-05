package org.kendar.ui.dto;

import org.kendar.plugins.base.AlwaysActivePlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ProtocolStatusDto {
    private List<ProtocolDto> protocols = new ArrayList<ProtocolDto>();

    public List<PluginDto> getActivePlugins() {
        var result = new ArrayList<PluginDto>();
        for(var protocol : protocols) {
            for(var plugin : protocol.getPlugins()) {
                if(plugin.isActive()) {
                    result.add(plugin);
                }
            }
        }
        return result;
    }

    public List<String> getPluginGroups () {
        var result = new HashSet<String>();
        for(var protocol : protocols) {
            for(var plugin : protocol.getPlugins()) {
                if(AlwaysActivePlugin.class.isAssignableFrom(plugin.getClass())) {
                    continue;
                }
                result.add(plugin.getId());
            }
        }
        return result.stream().sorted().toList();
    }

    public List<ProtocolDto> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolDto> protocols) {
        this.protocols = protocols;
    }
}
