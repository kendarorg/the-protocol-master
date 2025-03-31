package org.kendar.plugins.settings;

import org.kendar.plugins.settings.dtos.RestPluginsInterceptor;
import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class BasicRestPluginsPluginSettings extends PluginSettings {
    private List<RestPluginsInterceptor> interceptors = new ArrayList<>();

    public List<RestPluginsInterceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<RestPluginsInterceptor> interceptors) {
        this.interceptors = interceptors;
    }
}
