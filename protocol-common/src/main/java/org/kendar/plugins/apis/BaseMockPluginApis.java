package org.kendar.plugins.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.MockPlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;

import java.util.ArrayList;

import static org.kendar.apis.ApiUtils.respondJson;

@HttpTypeFilter(hostAddress = "*")
public class BaseMockPluginApis extends ProtocolPluginApiHandlerDefault<MockPlugin> {

    public BaseMockPluginApis(MockPlugin descriptor, String id, String instanceId) {
        super(descriptor, id, instanceId);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks")
    @TpmDoc(
            description = "Retrieve all the mock ids ",
            responses = @TpmResponse(
                    body = String[].class,
                    description = "Retrieve all the mock ids"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
    public boolean listAllMocks(Request reqp, Response resp) {

        var result = new ArrayList<>(getDescriptor().getMocks().keySet());
        respondJson(resp, result);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mockfile}",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mockfile}")
    @TpmDoc(
            description = "Retrieve the mock file",
            path = {@PathParameter(key = "mockfile")},
            responses = @TpmResponse(
                    body = MockStorage.class,
                    description = "Retrieve the mock file"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
    public boolean getSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("mockfile");
        var result = getDescriptor().getMocks().get(mockfile);
        respondJson(resp, result);
        return true;
    }

}
