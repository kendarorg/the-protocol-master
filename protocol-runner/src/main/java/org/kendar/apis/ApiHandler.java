package org.kendar.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmConstructor;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.dtos.PluginIndex;
import org.kendar.apis.dtos.ProtocolIndex;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.lang.System.exit;
import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@TpmService
@HttpTypeFilter(hostAddress = "*")
public class ApiHandler implements FilteringClass {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalSettings settings;
    private final ConcurrentLinkedQueue<ProtocolInstance> instances = new ConcurrentLinkedQueue<>();
    private List<GlobalPluginDescriptor> globalPlugins = new ArrayList<>();

    @TpmConstructor
    public ApiHandler(GlobalSettings settings, List<GlobalPluginDescriptor> globalPlugins) {
        this.settings = settings;
        this.globalPlugins = globalPlugins;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols",
            method = "GET", id = "GET /api/protocols")
    @TpmDoc(
            description = "Retrieve the protocols",
            responses = @TpmResponse(
                    body = ProtocolIndex[].class,
                    description = "List of protocols"
            ),
            tags = {"base/utils"})
    public void getProtocols(Request reqp, Response resp) {
        var result = instances.stream().map(p -> new
                        ProtocolIndex(p.getProtocolInstanceId(), p.getProtocol())).
                collect(Collectors.toList());
        respondJson(resp, result);
    }

    @HttpMethodFilter(
            pathAddress = "/api/status",
            method = "GET", id = "GET /api/status")
    @TpmDoc(
            description = "Retrieve the status of the application",
            responses = @TpmResponse(
                    body = String.class
            ),
            tags = {"base/utils"})
    public boolean getStatus(Request reqp, Response resp) {
        respondJson(resp, new Ok());
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/settings",
            method = "GET", id = "GET /api/global/settings")
    @TpmDoc(
            description = "Retrieve the settings of the application",
            responses = @TpmResponse(
                    body = String.class,
                    content = ConstantsMime.JSON
            ),
            tags = {"base/utils"})
    public boolean getSettings(Request reqp, Response resp) {
        respondJson(resp, settings);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins",
            method = "GET", id = "GET /api/global/plugins")
    @TpmDoc(
            description = "Retrieve the global plugins with status",
            responses = @TpmResponse(
                    body = PluginIndex[].class,
                    description = "List of global plugins"
            ),
            tags = {"plugins/global"})
    public void getGlobalPlugins(Request reqp, Response resp) {
        var result = globalPlugins.stream().map(p -> new
                        PluginIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
        respondJson(resp, result);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{id}/plugins",
            method = "GET", id = "GET /api/protocols/{id}/plugins")
    @TpmDoc(
            description = "List of protocol specific plugins",
            path = @PathParameter(key = "id"),
            responses = @TpmResponse(
                    body = PluginIndex[].class
            ),
            tags = {"plugins/protocols"})
    public void getProtocolPlugins(Request reqp, Response resp) {
        var protocolInstanceId = reqp.getPathParameter("id");
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        var result = instance.get().getPlugins().stream().map(p -> new
                        PluginIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
        respondJson(resp, result);
    }


    @SuppressWarnings("finally")
    @HttpMethodFilter(
            pathAddress = "/api/global/terminate",
            method = "GET", id = "GET /api/global/terminate")
    @TpmDoc(
            description = "Shutdown",
            responses = @TpmResponse(
                    body = Ok.class
            ),
            tags = {"base/utils"})
    public void terminate(Request reqp, Response resp) {
        try {
            respondOk(resp);
        } finally {
            for (var plugin : instances) {
                plugin.getServer().stop();
            }
            exit(0);
        }
    }

    public ConcurrentLinkedQueue<ProtocolInstance> getInstances() {
        return instances;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/all/plugins/{plugin}/{action}",
            method = "GET", id = "GET /api/protocols/all/plugins/{plugin}/{action}")
    @TpmDoc(
            description = "Execute action on all matching plugins",
            path = {@PathParameter(key = "plugin", description = "The plugin id"),
                    @PathParameter(key = "action",
                            allowedValues = {"start", "stop"},
                            description = "The action, start,stop,status"),
            },
            responses = {@TpmResponse(
                    body = Ok.class,
                    description = "In case of start/stop requests"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/protocols"})
    public boolean actionOnAllPlugins(Request reqp, Response resp) {
        var plugin = reqp.getPathParameter("plugin");
        var action = reqp.getPathParameter("action");

        {
            for (var instance : instances) {
                var pluginInstance = instance.getPlugins().stream().filter(p ->
                        p.getId().equalsIgnoreCase(plugin)).findFirst();
                if (pluginInstance.isEmpty()) continue;
                if (action.equalsIgnoreCase("start")) {
                    pluginInstance.get().setActive(true);
                } else if (action.equalsIgnoreCase("stop")) {
                    pluginInstance.get().setActive(false);
                }
            }
            respondOk(resp);
            return true;
        }
    }

    public void addGLobalPlugins(List<GlobalPluginDescriptor> globalPlugins) {
        this.globalPlugins.addAll(globalPlugins);
    }

    public void addProtocol(ProtocolInstance pi) {
        instances.add(pi);
    }
}