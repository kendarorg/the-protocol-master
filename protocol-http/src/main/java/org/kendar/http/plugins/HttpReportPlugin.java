package org.kendar.http.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.ReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;

import java.util.Map;

public class HttpReportPlugin extends ReportPlugin<PluginSettings> {
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (!isActive()) return false;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                in.getMethod() + " " + in.getProtocol() + "://" + in.getHost() + in.getPath(),
                connectionId,
                pluginContext.getStart(),
                duration,
                Map.of(
                        "query", in.getQuery() + "",
                        "contentType", in.getFirstHeader("content-type", "unknown"),
                            "requestSize",in.getSize()+"",
                        "returnType", out.getFirstHeader("content-type", "unknown"),
                        "responseSize",out.getSize()+"")
        ));
        return false;
    }

    @Override
    public String getProtocol() {
        return "http";
    }
}
