package org.kendar.dns.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

import java.util.List;
import java.util.Map;

@Extension
@TpmService(tags = "dns")
public class DnsReportPlugin extends ReportPlugin<PluginSettings> {
    public DnsReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "dns";
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, String requestedDomain, List<String> out) {
        if (!isActive()) return false;
        var connectionId = pluginContext.getIndex();
        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                phase == ProtocolPhase.PRE_CALL ? "request" : "response",
                connectionId,
                pluginContext.getStart(),
                phase == ProtocolPhase.PRE_CALL ? 0 : duration,
                Map.of(
                        "requestedDomain", requestedDomain,
                        "ips", String.join(",", out)
                )
        ));
        return false;
    }
}
