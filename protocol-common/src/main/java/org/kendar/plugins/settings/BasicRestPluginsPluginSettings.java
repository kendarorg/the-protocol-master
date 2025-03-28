package org.kendar.plugins.settings;

import org.kendar.plugins.settings.dtos.RestPluginsInterceptorDefinition;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class BasicRestPluginsPluginSettings extends PluginSettings {
    private List<RestPluginsInterceptorDefinition> interceptors = new ArrayList<>();

    public List<RestPluginsInterceptorDefinition> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<RestPluginsInterceptorDefinition> interceptors) {
        this.interceptors = interceptors;
    }
}
