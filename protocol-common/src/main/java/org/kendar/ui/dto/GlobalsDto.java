package org.kendar.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class GlobalsDto {
    private List<PluginDto> plugins = new ArrayList<>();

    public List<PluginDto> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PluginDto> plugins) {
        this.plugins = plugins;
    }
}
