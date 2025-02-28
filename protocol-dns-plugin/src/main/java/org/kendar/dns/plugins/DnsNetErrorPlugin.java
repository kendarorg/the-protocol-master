package org.kendar.dns.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "dns")
public class DnsNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public DnsNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }
}
