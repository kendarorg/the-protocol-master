package org.kendar.plugins;

import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.utils.JsonMapper;

import java.util.Map;

public abstract class JdbcReportPlugin extends ReportPlugin<PluginSettings> {

    public JdbcReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, JdbcCall in, SelectResult out) {
        if (!isActive()) return false;
        var context = pluginContext.getContext();
        var connectionId = context.getContextId();
        var duration = System.currentTimeMillis() - pluginContext.getStart();
        EventsQueue.send(new ReportDataEvent(
                getInstanceId(),
                getProtocol(),
                in.getQuery(),
                connectionId,
                pluginContext.getStart(),
                duration,
                Map.of("inputsCount", in.getParameterValues().size() + "",
                        "resultsCount", out.getCount() + "")
        ));
        return false;
    }

}
