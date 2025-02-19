package org.kendar.plugins.apis;

import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.MockPlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.storage.generic.StorageRepository;

import java.util.ArrayList;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter()
public class BaseMockPluginApis extends ProtocolPluginApiHandlerDefault<MockPlugin> {

    private final StorageRepository repository;

    public BaseMockPluginApis(MockPlugin descriptor, String id, String instanceId, StorageRepository repository) {
        super(descriptor, id, instanceId);
        this.repository = repository;
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/mock-plugin"})
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/mock-plugin"})
    public boolean getSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("mockfile");
        var result = getDescriptor().getMocks().get(mockfile);
        respondJson(resp, result);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mockfile}",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mockfile}")
    @TpmDoc(
            description = "Update/insert the mock file",
            path = {@PathParameter(key = "mockfile")},
            requests = @TpmRequest(body = MockStorage.class),
            responses ={@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/mock-plugin"})
    public boolean putSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("mockfile");
        var inputData = ((TextNode) reqp.getRequestText()).textValue();
        var inputObject = mapper.deserialize(inputData, MockStorage.class);
        getDescriptor().putMock(mockfile,inputObject);
        respondOk(resp);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mockfile}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mockfile}")
    @TpmDoc(
            description = "Remove the mock file",
            path = {@PathParameter(key = "mockfile")},
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/mock-plugin"})
    public boolean delSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("mockfile");
        getDescriptor().delMock(mockfile);
        respondOk(resp);
        return true;
    }

}
