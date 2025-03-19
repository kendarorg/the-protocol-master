package org.kendar.plugins.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.BasicRewritePlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.plugins.apis.dtos.ReplacerItemFile;
import org.kendar.plugins.apis.dtos.TestReplacerItem;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.storage.PluginFileManager;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.ReplacerItem;
import org.kendar.utils.ReplacerItemInstance;

import static org.kendar.apis.ApiUtils.*;

@HttpTypeFilter()
public class BaseRewritePluginApis extends ProtocolPluginApiHandlerDefault<BasicRewritePlugin> {


    private final PluginFileManager storage;
    private final MultiTemplateEngine resolversFactory;

    public BaseRewritePluginApis(BasicRewritePlugin descriptor, String id, String instanceId,
                                 PluginFileManager storage, MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.storage = storage;
        this.resolversFactory = resolversFactory;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite")
    @TpmDoc(
            description = "Retrieve all the rewrite ids ",
            responses = @TpmResponse(
                    body = String[].class,
                    description = "Retrieve all the rewrite ids"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/rewrite-plugin"})
    public boolean listAllMocks(Request reqp, Response resp) {

        var result = storage.listFiles();
        respondJson(resp, result);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite/{rewrite}",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite/{rewrite}")
    @TpmDoc(
            description = "Retrieve the rewrite file",
            path = {@PathParameter(key = "rewrite")},
            responses = @TpmResponse(
                    body = ReplacerItem.class,
                    description = "Retrieve the rewrite file"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/rewrite-plugin"})
    public boolean getSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("rewrite");
        var result = mapper.deserialize(storage.readFile(mockfile), ReplacerItem.class);
        respondJson(resp, result);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite/{rewrite}",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite/{rewrite}")
    @TpmDoc(
            description = "Update/insert the rewrite file",
            path = {@PathParameter(key = "rewrite")},
            requests = @TpmRequest(body = ReplacerItem.class),
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/rewrite-plugin"})
    public boolean putSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("rewrite");
        var inputData = reqp.getRequestText().toString();
        mapper.deserialize(inputData, MockStorage.class);
        storage.writeFile(mockfile, inputData);
        respondOk(resp);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/test",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/test")
    @TpmDoc(
            description = "Test the expression",
            requests = @TpmRequest(body = TestReplacerItem.class),
            responses = {@TpmResponse(
                    body = String.class
            ), @TpmResponse(
                    code = 404,
                    body = Ko.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/rewrite-plugin"})
    public void testReplacement(Request reqp, Response resp) {
        try {
            var data = reqp.getRequestText().toString();
            var toCheck = mapper.deserialize(data, TestReplacerItem.class);
            var instance = new ReplacerItemInstance(toCheck, false);
            var result = instance.run(toCheck.getTestTarget());
            if (!toCheck.getTestTarget().equalsIgnoreCase(result)) {
                respondText(resp,result);
            }else{
                respondKo(resp,"Not matched");
            }
        }catch (Exception ex){
            respondKo(resp,ex.getMessage());
        }
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite/{rewrite}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/rewrite/{rewrite}")
    @TpmDoc(
            description = "Remove the rewrite file",
            path = {@PathParameter(key = "rewrite")},
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/rewrite-plugin"})
    public boolean delSingleMock(Request reqp, Response resp) {
        var mockfile = reqp.getPathParameter("rewrite");
        storage.deleteFile(mockfile);
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
            file = mapper.serialize(new ReplacerItem());
            storage.writeFile(fileId, file);
        }
        var model = new ReplacerItemFile(getProtocolInstanceId(),fileId, mapper.deserialize(file, ReplacerItem.class));
        resolversFactory.render("generic/rewrite_plugin/single.jte",model,response);
    }

}
