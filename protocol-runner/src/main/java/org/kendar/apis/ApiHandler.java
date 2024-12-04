package org.kendar.apis;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.kendar.annotations.HamDoc;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.multi.HamRequest;
import org.kendar.annotations.multi.HamResponse;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.dtos.PluginIndex;
import org.kendar.apis.dtos.ProtocolIndex;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.lang.System.exit;

@HttpTypeFilter(hostAddress = "*")
public class ApiHandler implements FilteringClass {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalSettings settings;
    private final ConcurrentLinkedQueue<ProtocolInstance> instances = new ConcurrentLinkedQueue<>();
    private final List<GlobalPluginDescriptor> globalPlugins = new ArrayList<>();

    public ApiHandler(GlobalSettings settings) {
        this.settings = settings;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols",
            method = "GET", id = "GET /api/protocols")
    @HamDoc(
            description = "Retrieve the protocols",
            responses = @HamResponse(
                    body = ProtocolIndex[].class,
                    description = "List of protocols"
            ),
            tags = {"base/utils"})
    public void getProtocols(Request reqp, Response resp) {
        var result = instances.stream().map(p -> new
                        ProtocolIndex(p.getProtocolInstanceId(), p.getProtocol())).
                collect(Collectors.toList());
        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
        resp.setResponseText(mapper.toJsonNode(result));
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins",
            method = "GET", id = "GET /api/global/plugins")
    @HamDoc(
            description = "Retrieve the global plugins with status",
            responses = @HamResponse(
                    body = PluginIndex[].class,
                    description = "List of global plugins"
            ),
            tags = {"base/utils"})
    public void getGlobalPlugins(Request reqp, Response resp) {
        var result = globalPlugins.stream().map(p -> new
                        PluginIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
        resp.setResponseText(mapper.toJsonNode(result));
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{id}/plugins",
            method = "GET", id = "GET /api/protocols/{id}/plugins")
    @HamDoc(
            description = "List of protocol specific plugins",
            path = @PathParameter(key = "id"),
            responses = @HamResponse(
                    body = PluginIndex[].class,
                    description = ""
            ),
            tags = {"base/utils"})
    public void getProtocolPlugins(Request reqp, Response resp) {
        var protocolInstanceId = reqp.getPathParameter("id");
        var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
        var result = instance.get().getPlugins().stream().map(p -> new
                        PluginIndex(p.getId(), p.isActive())).
                collect(Collectors.toList());
        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
        resp.setResponseText(mapper.toJsonNode(result));
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage",
            method = "GET", id = "GET /api/global/storage")
    @HamDoc(
            description = "Download recorded data",
            responses = {@HamResponse(
                    content = ConstantsMime.ZIP,
                    body = byte[].class
            ),@HamResponse(
                    body = Ok.class

            ),@HamResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/utils"})
    public boolean handleDownload(Request reqp, Response resp) {
        StorageRepository storage = settings.getService("storage");
        Object result = new Ok();
        try {
            var data = storage.readAsZip();
            resp.setResponseText(new BinaryNode(data));
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.ZIP);
            resp.addHeader("Content-Transfer-Encoding", "binary");
            resp.addHeader("Content-Disposition", "attachment; filename=\"storage.zip\";");
        } catch (Exception ex) {
            result = new Ko(ex.getMessage());
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(result));
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage",
            method = "POST", id = "POST /api/global/storage")
    @HamDoc(
            description = "Upload existing recording",
            requests = @HamRequest(
                    accept = ConstantsMime.ZIP
            ),
            responses = {@HamResponse(
                    body = Ok.class
            ),@HamResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/utils"})
    public boolean handleUpload(Request reqp, Response resp) {
        StorageRepository storage = settings.getService("storage");
        Object result = new Ok();
        try {

            byte[] inputData;
            if (reqp.getRequestText() instanceof BinaryNode) {
                inputData = ((BinaryNode) reqp.getRequestText()).binaryValue();
            } else {
                inputData = reqp.getRequestText().textValue().getBytes();
            }
            storage.writeZip(inputData);
            storage.initialize();
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(result));

        } catch (Exception ex) {
            result = new Ko(ex.getMessage());
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(result));
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/terminate",
            method = "GET", id = "GET /api/global/terminate")
    @HamDoc(
            description = "Shutdown",
            responses = @HamResponse(
                    body = Ok.class
            ),
            tags = {"base/utils"})
    public void terminate(Request reqp, Response resp) {
        try {
            var result = new Ok();
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(result));
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
            pathAddress = "/api/protocols/{id}/plugins/{plugin}/{action}",
            method = "GET", id = "GET /api/protocols/{id}/plugins/{plugin}/{action}")
    @HamDoc(
            description = "Execute action on specific plugin(or all matching setting" +
                    "the protocol to *)",
            path = {@PathParameter(key = "id", description = "* to act on all protocols"),
                    @PathParameter(key = "plugin", description = "The plugin id"),
                    @PathParameter(key = "action",
                            allowedValues = {"start","stop","status"},
                            description = "The action, start,stop,status"),
            },
            responses = {@HamResponse(
                    body = Ok.class,
                    description = "In case of start/stop requests"
            ),@HamResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            ),@HamResponse(
                    body = Status.class,
                    description = "In case of status reqest"
            )},
            tags = {"base/utils"})
    public boolean actionOnAllPlugins(Request reqp, Response resp) {
        var protocolInstanceId = reqp.getPathParameter("id");
        var plugin = reqp.getPathParameter("plugin");
        var action = reqp.getPathParameter("action");
        if (protocolInstanceId.equalsIgnoreCase("*")) {
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
            resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
            resp.setResponseText(mapper.toJsonNode(new Ok()));
            return true;
        } else {
            var instance = instances.stream().filter(p -> p.getProtocolInstanceId().equals(protocolInstanceId)).findFirst();
            if (instance.isEmpty()) {

                resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                resp.setResponseText(mapper.toJsonNode(new Ko("Missing protocol " + protocolInstanceId)));
                return true;
            } else {
                var pluginInstance = instance.get().getPlugins().stream().filter(p ->
                        p.getId().equalsIgnoreCase(plugin)).findFirst();
                if (pluginInstance.isEmpty()) {
                    resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                    resp.setResponseText(mapper.toJsonNode(new Ko("Missing plugin " + plugin + " for protocol " + protocolInstanceId)));
                } else {
                    if (action.equalsIgnoreCase("start")) {
                        pluginInstance.get().setActive(true);
                        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                        resp.setResponseText(mapper.toJsonNode(new Ok()));
                        return true;
                    } else if (action.equalsIgnoreCase("stop")) {
                        pluginInstance.get().setActive(false);
                        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                        resp.setResponseText(mapper.toJsonNode(new Ok()));
                        return true;
                    } else if (action.equalsIgnoreCase("status")) {
                        var status = new Status();
                        status.setActive(pluginInstance.get().isActive());
                        resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                        resp.setResponseText(mapper.toJsonNode(status));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void addGLobalPlugins(List<GlobalPluginDescriptor> globalPlugins) {
        this.globalPlugins.addAll(globalPlugins);
    }

    public void addProtocol(ProtocolInstance pi) {
        instances.add(pi);
    }
}