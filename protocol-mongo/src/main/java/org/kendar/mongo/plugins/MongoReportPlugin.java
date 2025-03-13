package org.kendar.mongo.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mongo.dtos.MongoCommandsConstants;
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.plugins.BasicReportPlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.proxy.PluginContext;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;

import java.util.Map;

@TpmService(tags = "mongodb")
public class MongoReportPlugin extends BasicReportPlugin<PluginSettings> {

    public MongoReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, OpMsgContent in, OpMsgContent out) {
        if (!isActive()) return false;
        if (in.getSections() != null && !in.getSections().isEmpty()) {
            var queryType = "unknownCommand";
            var collection = "unknownCollection";
            var jsonMap = mapper.toJsonNode(in.getSections().get(0).getDocuments().get(0));
            for (var value : MongoCommandsConstants.values()) {
                if (jsonMap.has(value.name())) {
                    queryType = value.name();
                    collection = jsonMap.get(value.name()).asText();
                    break;
                }
            }
            var query = mapper.serialize(mapper.toJsonNode(in.getSections().get(0).getDocuments().get(0)));
            var inputsCount = 0;
            if (in.getSections().size() > 1) {
                inputsCount = in.getSections().get(1).getDocuments().size();
            }

            var resultsCount = 0;


            if (out.getSections() != null && !out.getSections().isEmpty()) {
                resultsCount = out.getSections().get(0).getDocuments().size();
            }


            var context = pluginContext.getContext();
            var connectionId = context.getContextId();
            var duration = System.currentTimeMillis() - pluginContext.getStart();
            EventsQueue.send(new ReportDataEvent(
                    getInstanceId(),
                    getProtocol(),
                    String.format("QUERY:%s:%s", queryType, collection),
                    connectionId,
                    pluginContext.getStart(),
                    duration,
                    Map.of("inputsCount", inputsCount + "",
                            "resultsCount", resultsCount + "",
                            "query", query)
            ));
        }

        return false;
    }

    @Override
    public String getProtocol() {
        return "mongodb";
    }
}
