package org.kendar.http.plugins;

import org.kendar.annotations.di.TpmService;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;

import java.util.Map;

@TpmService(tags = "http")
public class HttpReportPlugin extends ReportPlugin<PluginSettings> {

    public HttpReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (!isActive()) return false;

        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                in.getMethod() + " " + in.getProtocol() + "://" + in.getHost() + in.getPath(),
                0,
                pluginContext.getStart(),
                duration,
                Map.of(
                        "query", in.getQuery() + "",
                        "contentType", in.getFirstHeader("content-type", "unknown"),
                        "requestSize", in.getSize() + "",
                        "returnType", out.getFirstHeader("content-type", "unknown"),
                        "responseSize", out.getSize() + "")
        ));
        return false;
    }

    @Override
    public String getProtocol() {
        return "http";
    }
}
