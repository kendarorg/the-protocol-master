package org.kendar.http.plugins;

import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.http.plugins.settings.HttpReportPluginSettings;
import org.kendar.plugins.BasicReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.utils.JsonMapper;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;

@TpmService(tags = "http")
public class HttpReportPlugin extends BasicReportPlugin<HttpReportPluginSettings> {
    private final HashSet<String> localIgnore = new HashSet<>();

    public HttpReportPlugin(JsonMapper mapper) {
        super(mapper);
        localIgnore.add("127.0.0.1");
        localIgnore.add("localhost");

        tryAddName(localIgnore, () -> InetAddress.getLocalHost().getCanonicalHostName());
        tryAddName(localIgnore, () -> InetAddress.getLocalHost().getHostName());
        tryAddName(localIgnore, () -> InetAddress.getLocalHost().getHostAddress());
        tryAddName(localIgnore, () -> InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).getHostAddress());
    }

    private static void tryAddName(HashSet<String> toFill, MySupplier supplier) {
        try {
            var data = supplier.get();
            toFill.add(data);
        } catch (Exception e) {
        }
    }

    @Override
    public Class<?> getSettingClass() {
        return HttpReportPluginSettings.class;
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {
        if (!isActive()) return false;
        if (getSettings().isIgnoreTpm()) {
            if (localIgnore.contains(in.getHost())) {
                return false;
            }
        }
        for (var ignored : getSettings().getIgnore()) {
            if (ignored.equalsIgnoreCase(in.getHost())) {
                return false;
            }
        }

        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                in.getProtocol() + "://" + in.getHost() + in.getPath(),
                0,
                pluginContext.getStart(),
                duration,
                Map.of(
                        "method", in.getMethod(),
                        "host", in.getHost(),
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

    public interface MySupplier {
        String get() throws Exception;
    }
}
