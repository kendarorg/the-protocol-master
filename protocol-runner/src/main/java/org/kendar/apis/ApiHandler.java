package org.kendar.apis;

import org.kendar.plugins.base.TPMPluginFile;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kendar.VersionChecker;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.dtos.PluginIndex;
import org.kendar.apis.dtos.ProtocolIndex;
import org.kendar.apis.dtos.StringKvp;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.events.EventsQueue;
import org.kendar.events.RestartEvent;
import org.kendar.events.StorageReloadedEvent;
import org.kendar.events.TerminateEvent;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.pf4j.PluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.kendar.apis.ApiUtils.*;

@TpmService
@HttpTypeFilter()
public class ApiHandler implements FilteringClass {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalSettings settings;
    private final ConcurrentLinkedQueue<ProtocolInstance> instances = new ConcurrentLinkedQueue<>();
    private final StorageRepository repository;
    private final PluginManager pluginManager;
    private List<GlobalPluginDescriptor> globalPlugins = new ArrayList<>();

    @TpmConstructor
    public ApiHandler(GlobalSettings settings, List<GlobalPluginDescriptor> globalPlugins,
                      StorageRepository repository, PluginManager pluginManager) {
        this.settings = settings;
        this.globalPlugins = globalPlugins;
        this.repository = repository;
        this.pluginManager = pluginManager;
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
                        ProtocolIndex(p.getInstanceId(), p.getProtocol())).
                collect(Collectors.toList());
        respondJson(resp, result);
    }

    @HttpMethodFilter(
            pathAddress = "/api/status",
            method = "GET", id = "GET /api/status")
    @TpmDoc(
            description = "Retrieve the status of the application",
            responses = @TpmResponse(
                    body = Ok.class
            ),
            tags = {"base/utils"})
    public boolean getStatus(Request reqp, Response resp) {
        respondJson(resp, new Ok());
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/version",
            method = "GET", id = "GET /api/version")
    @TpmDoc(
            description = "Retrieve the version of the application",
            responses = @TpmResponse(
                    body = StringKvp[].class
            ),
            tags = {"base/utils"})
    public void getVersion(Request reqp, Response resp) {
        var result = new ArrayList<StringKvp>();
        result.add(new StringKvp("protocol-runner",VersionChecker.getTpmVersion()));
        for(var plugin:pluginManager.getPlugins()) {
            var pluginDescriptor = (TPMPluginFile)plugin.getPlugin();
            result.add(new StringKvp(pluginDescriptor.getTpmPluginName(),pluginDescriptor.getTpmPluginVersion()));
        }
        respondJson(resp, result);
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/settings",
            method = "GET", id = "GET /api/global/settings")
    @TpmDoc(
            description = "Retrieve the settings of the application",
            responses = @TpmResponse(
                    body = String.class
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
        var instance = instances.stream().filter(p -> p.getInstanceId().equals(protocolInstanceId))
                .findFirst();
        if (instance.isEmpty()) {
            respondJson(resp, List.of());
            return;
        }
        var result = instance.get().getPlugins().stream().map(p -> new
                        PluginIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
        respondJson(resp, result);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{instanceId}",
            method = "POST", id = "POST /api/protocols/{instanceId}")
    @TpmDoc(
            description = "Update protocol settings, restart when saving",
            path = @PathParameter(key = "instanceId"),
            requests = @TpmRequest(body = String.class),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/protocols"})
    public void updateProtocolPlugins(Request reqp, Response resp) {
        var protocolInstanceId = reqp.getPathParameter("instanceId");
        var instance = instances.stream().filter(p -> p.getInstanceId().equals(protocolInstanceId))
                .findFirst();
        var protocolSettings = (ObjectNode) mapper.toJsonNode(instance.get().getSettings());
        var inputData = (ObjectNode) mapper.toJsonNode(reqp.getRequestText().toString());
        var iterator = inputData.fields();
        while (iterator.hasNext()) {
            var field = iterator.next();
            protocolSettings.set(field.getKey(), field.getValue());
        }
        var serializedSettings = (ObjectNode) mapper.toJsonNode(settings);
        ((ObjectNode)serializedSettings.get("protocols")).set(protocolInstanceId, protocolSettings);

        var stringSettings = mapper.serialize(serializedSettings);
        try {
            var storage = DiService.getThreadContext().getInstance(StorageRepository.class);
            Files.writeString(Path.of("settings.json"), stringSettings);
            storage.writeFile(stringSettings, "settings");
            EventsQueue.send(new StorageReloadedEvent().withSettings("settings.json"));
            respondOk(resp);
        } catch (IOException e) {
            respondKo(resp, e);
        }

    }

    @HttpMethodFilter(
            pathAddress = "/api/global/restart",
            method = "GET", id = "GET /api/global/restart")
    @TpmDoc(
            description = "Restart",
            responses = @TpmResponse(
                    body = Ok.class
            ),
            tags = {"base/utils"})
    public void restart(Request reqp, Response resp) {
        try {
            respondOk(resp);
        } finally {
            for (var plugin : instances) {
                plugin.getServer().stop();
            }
            EventsQueue.send(new RestartEvent());
        }
    }


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
            EventsQueue.send(new TerminateEvent());
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


    public void addProtocol(ProtocolInstance pi) {
        instances.add(pi);
    }
}