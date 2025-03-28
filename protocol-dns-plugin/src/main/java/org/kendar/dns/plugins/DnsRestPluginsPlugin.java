package org.kendar.dns.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "dns")
public class DnsRestPluginsPlugin extends BasicRestPluginsPlugin {
    public DnsRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "dns";
    }
}
