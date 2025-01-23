package org.kendar.plugins;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.utils.JsonMapper;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter()
public class GlobalReportPluginApiHandler implements BasePluginApiHandler {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalReportPlugin plugin;

    public GlobalReportPluginApiHandler(GlobalReportPlugin plugin) {

        this.plugin = plugin;
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
}
