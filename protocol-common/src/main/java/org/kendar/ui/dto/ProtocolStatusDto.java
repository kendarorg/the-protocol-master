package org.kendar.ui.dto;

import org.kendar.plugins.base.AlwaysActivePlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProtocolStatusDto extends BaseHtmxDto {
    private List<ProtocolDto> protocols = new ArrayList<>();

    public List<PluginDto> getActivePlugins() {
        var result = new ArrayList<PluginDto>();
        for (var protocol : protocols) {
            for (var plugin : protocol.getPlugins()) {
                if (plugin.isActive()) {
                    result.add(plugin);
                }
            }
        }
        return result;
    }

    public List<WildcarPluginDto> getWildcardPlugins() {
        var partial = new HashMap<String, List<PluginDto>>();
        for (var protocol : protocols) {
            for (var plugin : protocol.getPlugins()) {
                if (AlwaysActivePlugin.class.isAssignableFrom(plugin.getClass())) {
                    continue;
                }
                if (!partial.containsKey(plugin.getId())) {
                    partial.put(plugin.getId(), new ArrayList<>());
                }
                partial.get(plugin.getId()).add(plugin);
            }
        }
        var result = new ArrayList<WildcarPluginDto>();
        for (var plugin : partial.values()) {
            var dto = new WildcarPluginDto(plugin.get(0).getId());
            var active = plugin.stream().filter(PluginDto::isActive).count();
            var inactive = plugin.size() - active;
            dto.setActive((int) active);
            dto.setNotActive((int) inactive);
            result.add(dto);
        }
        return result;
    }

    public List<ProtocolDto> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolDto> protocols) {
        this.protocols = protocols;
    }
}
