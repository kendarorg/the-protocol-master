package org.kendar.plugins.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.BasicMockPlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.plugins.apis.dtos.MockStorageFile;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.storage.PluginFileManager;
import org.kendar.ui.MultiTemplateEngine;

import java.util.ArrayList;
import java.util.Map;

import static org.kendar.apis.ApiUtils.*;


@HttpTypeFilter()
public class BasicMockPluginApis extends ProtocolPluginApiHandlerDefault<BasicMockPlugin> {


    private final PluginFileManager storage;
    private final MultiTemplateEngine resolversFactory;

    public BasicMockPluginApis(BasicMockPlugin descriptor, String id, String instanceId,
                               PluginFileManager storage, MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.storage = storage;
        this.resolversFactory = resolversFactory;
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
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/mock-plugin"})
    public void putSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("mockfile");
        var inputData = reqp.getRequestText().toString();
        var data = mapper.deserialize(inputData,MockStorage.class);
        var mocks = (Map<String,MockStorage>)getDescriptor().getMocks();

        for(var kvp : mocks.entrySet()) {
            if(kvp.getValue().getIndex()==data.getIndex() && !kvp.getKey().equals(mockfile)) {
                respondKo(resp,"Duplicate index "+data.getIndex());
                return;
            }
        }
        storage.writeFile(mockfile, inputData);
        getDescriptor().reloadData();
        respondOk(resp);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mock}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/mocks/{mock}")
    @TpmDoc(
            description = "Remove the mock file",
            path = {@PathParameter(key = "mock")},
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/mock-plugin"})
    public boolean delSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("mock");
        storage.deleteFile(mockfile);
        getDescriptor().reloadData();
        respondOk(resp);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/file/{id}",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/{id}")
    public void retrieveFile(Request reqp, Response response) {
        var fileId = reqp.getPathParameter("id");
        var file = storage.readFile(fileId);
        if(file == null) {
            file = mapper.serialize(new MockStorage());
            storage.writeFile(fileId, file);
        }
        var model = new MockStorageFile(getProtocolInstanceId(),fileId, mapper.deserialize(file, MockStorage.class));
        resolversFactory.render("generic/mock_plugin/single.jte",model,response);
    }

}
