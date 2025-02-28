package org.kendar.dns.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "dns")
public class DnsLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public DnsLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mqtt";
    }

}
