package org.kendar.plugins;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.kendar.annotations.HamDoc;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.multi.HamResponse;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.utils.JsonMapper;

@HttpTypeFilter(hostAddress = "*")
public class GlobalReportPluginApiHandler implements BasePluginApiHandler {
    private final GlobalReportPlugin plugin;

    public GlobalReportPluginApiHandler(GlobalReportPlugin plugin) {

        this.plugin = plugin;
    }
    private static JsonMapper mapper = new JsonMapper();

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins/report-plugin/{action}",
            method = "GET", id = "GET /api/global/plugins/report-plugin/{action}")
    @HamDoc(
            description = "Handle the global report plugin actions start,stop,status,download",
            path = @PathParameter(key = "action",
            allowedValues = {"start","stop","status","download"}),
            responses = {
                    @HamResponse(
                            body = Ok.class
                    ),
                    @HamResponse(
                            body = Status.class,
                            description = "In case of status reqest"
                    ),
                    @HamResponse(
                            body = GlobalReport.class,
                            description = "In case of download"
                    )
            },
            tags = {"base/utils"})
    public boolean handleGlobalPluginActions(Request reqp, Response resp) {
        var action = reqp.getPathParameter("action");
        if("download".equalsIgnoreCase(action)) {
            GlobalReport report = plugin.getReport();
            resp.setResponseText(new BinaryNode(mapper.serializePretty(report).getBytes()));
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.addHeader("Content-Transfer-Encoding", "binary");
            resp.addHeader("Content-Disposition", "attachment; filename=\"report.json\";");
            return true;
        }else if("start".equalsIgnoreCase(action)) {
            plugin.setActive(true);
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(new Ok()));
            return true;
        }else if("stop".equalsIgnoreCase(action)) {
            plugin.setActive(false);
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(new Ok()));
            return true;
        }else if("status".equalsIgnoreCase(action)) {
            var status= new Status();
            status.setActive(plugin.isActive());
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(status));
            return true;
        }
        return false;
    }

    @Override
    public String getId() {
        return "global."+plugin.getId();
    }
}
