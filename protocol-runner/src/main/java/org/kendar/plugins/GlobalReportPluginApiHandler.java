package org.kendar.plugins;

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
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.Token;

import java.util.ArrayList;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter()
public class GlobalReportPluginApiHandler implements BasePluginApiHandler {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalReportPlugin plugin;
    private final StorageRepository repository;
    private final SimpleParser simpleParser;

    public GlobalReportPluginApiHandler(GlobalReportPlugin plugin, StorageRepository repository, SimpleParser simpleParser) {

        this.plugin = plugin;
        this.repository = repository;
        this.simpleParser = simpleParser;
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
            description = "Handle the global report plugin data retrieval<br>Uses <a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> query language",
            query = {
                    @QueryString(key = "tpmql", description = "<a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> selection query", example = "")
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
        var tpmqlstring = reqp.getQuery("tpmql");
        Token tpmql = null;
        if (tpmqlstring != null && !tpmqlstring.isEmpty()) {
            tpmql = simpleParser.parse(tpmqlstring);
        }
        var result = new ArrayList<ReportDataEvent>();
        var allFiles = repository.listFiles("global", "report-plugin");
        for (var file : allFiles) {
            var text = repository.readFile("global", "report-plugin",file);
            var data = mapper.deserialize(text, ReportDataEvent.class);
            if (tpmql != null) {
                var toEvaluate = mapper.toJsonNode(text);
                if ((boolean) simpleParser.evaluate(tpmql, toEvaluate)) {
                    result.add(data);
                }
            } else {
                result.add(data);
            }
        }
        respondJson(resp, result);
        return true;
    }
}
