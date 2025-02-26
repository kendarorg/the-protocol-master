package org.kendar.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.QueryString;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.Map;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter()
public class GlobalReportPluginApiHandler implements BasePluginApiHandler {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalReportPlugin plugin;
    private final StorageRepository repository;

    public GlobalReportPluginApiHandler(GlobalReportPlugin plugin, StorageRepository repository) {

        this.plugin = plugin;
        this.repository = repository;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins/report-plugin/{action}",
            method = "GET", id = "GET /api/global/plugins/report-plugin/{action}")
    @TpmDoc(
            description = "Handle the global report plugin actions start,stop,status,download",
            path = @PathParameter(key = "action",
                    allowedValues = {"start", "stop", "status", "download"}),
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    ),
                    @TpmResponse(
                            body = Status.class,
                            description = "In case of status request"
                    ),
                    @TpmResponse(
                            body = GlobalReport.class,
                            description = "In case of download"
                    )
            },
            tags = {"plugins/global"})
    public boolean handleGlobalPluginActions(Request reqp, Response resp) {
        var action = reqp.getPathParameter("action");
        if ("download".equalsIgnoreCase(action)) {
            GlobalReport report = plugin.getReport();
            respondJson(resp, report);
            return true;
        } else if ("start".equalsIgnoreCase(action)) {
            plugin.setActive(true);
            respondOk(resp);
            return true;
        } else if ("stop".equalsIgnoreCase(action)) {
            plugin.setActive(false);
            respondOk(resp);
            return true;
        } else if ("status".equalsIgnoreCase(action)) {
            var status = new Status();
            status.setActive(plugin.isActive());
            respondJson(resp, status);
            return true;
        }
        return false;
    }



    @Override
    public String getId() {
        return "global." + plugin.getId();
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins/report-plugin/report",
            method = "GET", id = "GET /api/global/plugins/report-plugin/report")
    @TpmDoc(
            description = "Handle the global report plugin data retrieval",
            query = {
                    @QueryString(key = "map", description = "<a href='https://github.com/json-path/JsonPath'>JsonPath</a> expression"),
                    @QueryString(key = "reduce", description = "<a href='https://github.com/json-path/JsonPath'>JsonPath</a> expression")
            },
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    ),
                    @TpmResponse(
                            body = ReportDataEvent[].class,
                            description = "List of matching events"
                    )
            },
            tags = {"plugins/global"})
    public boolean loadReport(Request reqp, Response resp) {
        var result = new ArrayList<Map<String, Object>>();
        var mapJsonPath = reqp.getQuery("map");
        JsonPath map= null;
        if(mapJsonPath != null) {
            map = JsonPath.compile(mapJsonPath, new Predicate[]{});
        }

        var reduceJsonPath = reqp.getQuery("reduce");
        JsonPath reduce= null;
        if(reduceJsonPath != null) {
            reduce = JsonPath.compile(reduceJsonPath, new Predicate[]{});
        }
        var allFiles = repository.listPluginFiles("global","report-plugin");
        for(var file : allFiles) {
            var text = repository.readPluginFile(file);
            if(map != null) {
                Map<String, Object> mapped = map.read(text.getContent());
                if(mapped == null || mapped.isEmpty()) {
                    continue;
                }
            }
            if(reduce != null) {
                Map<String, Object> mapped = reduce.read(text.getContent());
                result.add(mapped);
            }else{
                Map<String, Object> mapped = mapper.deserialize(text.getContent(), new TypeReference<Map<String, Object>>() {});
                result.add(mapped);
            }
        }
        respondJson(resp, result);
        return true;
    }
}
