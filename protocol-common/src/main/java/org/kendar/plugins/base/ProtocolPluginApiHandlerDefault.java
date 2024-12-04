package org.kendar.plugins.base;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.apis.Status;
import org.kendar.utils.JsonMapper;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter(hostAddress = "*")
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
                    description = "In case of status reqest"
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
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

}
