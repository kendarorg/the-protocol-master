package org.kendar.plugins;

import com.sun.net.httpserver.HttpExchange;
import org.kendar.plugins.apis.FileDownload;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.plugins.base.BaseApiServerHandler;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.utils.JsonMapper;

import java.util.HashMap;
import java.util.Locale;

public class GlobalReportPluginApiHandler implements BasePluginApiHandler {
    private final GlobalReportPlugin plugin;

    public GlobalReportPluginApiHandler(GlobalReportPlugin plugin) {

        this.plugin = plugin;
    }
    private static JsonMapper mapper = new JsonMapper();
    @Override
    public boolean handle(BaseApiServerHandler apiServerHandler, HttpExchange exchange, String pathPart) {
        var parameters = new HashMap<String, String>();
        if (apiServerHandler.isPath(pathPart, "/{action}", parameters)) {
            var action = parameters.get("action").toLowerCase(Locale.ROOT);
            switch (action) {
                case "start":
                    plugin.setActive(true);
                    apiServerHandler.respond(exchange, new Ok(), 200);
                    return true;
                case "stop":
                    plugin.setActive(false);
                    apiServerHandler.respond(exchange, new Ok(), 200);
                    return true;
                case "status":
                    var status = new Status();
                    status.setActive(plugin.isActive());
                    apiServerHandler.respond(exchange, status, 200);
                    return true;
                case "download":
                    GlobalReport report = plugin.getReport();
                    var fd = new FileDownload();
                    fd.setContentType("application/json");
                    fd.setData(mapper.serializePretty(report).getBytes());
                    fd.setFileName("report.json");
                    apiServerHandler.respond(exchange, fd, 200);
                    return true;
                default:
                    apiServerHandler.respond(exchange, new Ko("Unknown action " + action), 404);
                    return true;
            }
        }
        return false;
    }

    @Override
    public String getId() {
        return plugin.getId();
    }
}
