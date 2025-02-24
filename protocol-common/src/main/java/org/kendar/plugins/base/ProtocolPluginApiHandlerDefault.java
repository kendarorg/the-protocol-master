package org.kendar.plugins.base;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.settings.PluginSettings;
import org.kendar.utils.JsonMapper;

import static org.kendar.apis.ApiUtils.*;


@HttpTypeFilter()
public class ProtocolPluginApiHandlerDefault<T extends ProtocolPluginDescriptor> implements ProtocolPluginApiHandler {
    protected static final JsonMapper mapper = new JsonMapper();
    private final T descriptor;
    private final String id;
    private final String instanceId;


    public ProtocolPluginApiHandlerDefault(T descriptor, String id, String instanceId) {
        this.descriptor = descriptor;
        this.id = id;
        this.instanceId = instanceId;
    }

    public T getDescriptor() {
        return descriptor;
    }

    public String getId() {
        return id + "." + instanceId;
    }

    public String getProtocolInstanceId() {
        return instanceId;
    }

    @Override
    public String getProtocol() {
        return descriptor.getProtocol();
    }

    @Override
    public String getPluginId() {
        return descriptor.getId();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/{action}",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/{action}/globals")
    @TpmDoc(
            description = "Execute action on specific plugin(or all matching setting" +
                    "the protocol to *)",
            path = {@PathParameter(key = "action",
                    allowedValues = {"start", "stop", "status"},
                    description = "The action, start,stop,status"),
            },
            responses = {@TpmResponse(
                    body = Ok.class,
                    description = "In case of start/stop requests"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            ), @TpmResponse(
                    body = Status.class,
                    description = "In case of status request"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/{#plugin}"})
    public boolean actionOnAllPlugins(Request reqp, Response resp) {
        var action = reqp.getPathParameter("action");
        {
            var pluginInstance = getDescriptor();
            {
                {
                    if (action.equalsIgnoreCase("start")) {
                        pluginInstance.setActive(true);
                        respondOk(resp);
                        return true;
                    } else if (action.equalsIgnoreCase("stop")) {
                        pluginInstance.setActive(false);
                        respondOk(resp);
                        return true;
                    } else if (action.equalsIgnoreCase("status")) {
                        var status = new Status();
                        status.setActive(pluginInstance.isActive());
                        respondJson(resp, status);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/settings",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/settings")
    @TpmDoc(
            description = "Retrieve the settings for the specific plugin",
            responses = {@TpmResponse(
                    bodyMethod = "getSettingsClass",
                    description = "The settings"
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/{#plugin}"})
    public boolean getSettings(Request reqp, Response resp) {
        var pluginInstance = getDescriptor();
        if (ProtocolPluginDescriptorBase.class.isAssignableFrom(pluginInstance.getClass())) {
            var ppdb = (ProtocolPluginDescriptorBase) pluginInstance;
            respondJson(resp, ppdb.getSettings());
        } else {
            respondKo(resp, "No plugin found with settings");
        }
        return true;
    }

    public Class<?> getSettingsClass() {
        return descriptor.getSettingClass();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/settings",
            method = "PUT", id = "PUT /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/settings")
    @TpmDoc(
            description = "Set the settings for the specific plugin",
            responses = {@TpmResponse(
                    code = 500,
                    body = Ko.class,
                    description = "In case of errors"
            )},
            requests = @TpmRequest(
                    bodyMethod = "getSettingsClass"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/{#plugin}"})
    public boolean setSettings(Request reqp, Response resp) {
        var pluginInstance = getDescriptor();
        if (ProtocolPluginDescriptorBase.class.isAssignableFrom(pluginInstance.getClass())) {
            var settings = mapper.deserialize(reqp.getRequestText().toString(), pluginInstance.getSettingClass());
            var ppdb = (ProtocolPluginDescriptorBase) pluginInstance;
            ppdb.setSettings((PluginSettings) settings);
            respondOk(resp);
        } else {
            respondKo(resp, "No plugin found with settings");
        }
        return true;
    }

}
