package org.kendar.http.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "http")
public class HttpRestPluginsPlugin extends BasicRestPluginsPlugin {
    public HttpRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "http";
    }
}
