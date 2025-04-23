package org.kendar.plugins.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.apis.dtos.RecordItemFile;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.storage.PluginFileManager;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.ReplacerItem;
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.Token;

import java.util.ArrayList;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter()
public class BasicRecordPluginApis extends ProtocolPluginApiHandlerDefault<BasicRecordPlugin> {


    private final PluginFileManager storage;
    private final MultiTemplateEngine resolversFactory;
    private final SimpleParser parser;

    public BasicRecordPluginApis(BasicRecordPlugin descriptor, String id, String instanceId,
                                 PluginFileManager storage, MultiTemplateEngine resolversFactory,
                                 SimpleParser parser) {
        super(descriptor, id, instanceId);
        this.storage = storage;
        this.resolversFactory = resolversFactory;
        this.parser = parser;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record")
    @TpmDoc(
            description = "Retrieve all the record ids ",
            responses = @TpmResponse(
                    body = String[].class,
                    description = "Retrieve all the record ids"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/record-plugin"})
    public boolean listAllFiles(Request req, Response resp) {

        var result = storage.listFiles();
        respondJson(resp, result);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record/{record}",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record/{record}")
    @TpmDoc(
            description = "Retrieve the record file",
            path = {@PathParameter(key = "record")},
            responses = @TpmResponse(
                    body = ReplacerItem.class,
                    description = "Retrieve the record file"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/record-plugin"})
    public void getSingleFile(Request req, Response resp) {
        var record = req.getPathParameter("record");
        respondJson(resp, new RecordItemFile(getProtocolInstanceId(), storage.getRepository().readFile("scenario", record), record));
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record/{record}",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record/{record}")
    @TpmDoc(
            description = "Update/insert the record file",
            path = {@PathParameter(key = "record")},
            requests = @TpmRequest(body = ReplacerItem.class),
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/record-plugin"})
    public void putSingleFile(Request req, Response resp) {
        var record = req.getPathParameter("record");
        var inputData = req.getRequestText().toString();
        storage.getRepository().writeFile(inputData, "scenario", record);
        respondOk(resp);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record/{record}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/record/{record}")
    @TpmDoc(
            description = "Remove the record file",
            path = {@PathParameter(key = "record")},
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/record-plugin"})
    public void delSingleMock(Request req, Response resp) {
        var record = req.getPathParameter("record");
        storage.getRepository().deleteFile("scenario", record);
        respondOk(resp);
    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/file/{id}",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/{id}")
    public void retrieveFile(Request req, Response response) {
        var fileId = req.getPathParameter("id");
        var file = storage.getRepository().readFile("scenario", fileId);
        if (file == null) {
            file = "{}";
            storage.writeFile(fileId, file);
        }
        var model = new RecordItemFile(getProtocolInstanceId(), file, fileId);
        resolversFactory.render("generic/record_plugin/single.jte", model, response);
    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/file",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/file")
    public void retrieveFiles(Request req, Response response) {
        var tpmqlstring = req.getQuery("tpmql");
        Token tpmql = null;
        if (tpmqlstring != null && !tpmqlstring.isEmpty()) {
            tpmql = parser.parse(tpmqlstring);
        }


        var files = storage.getRepository().listFiles("scenario");
        var result = new ArrayList<String>();


        for (var file : files) {
            if (!file.endsWith("." + getProtocolInstanceId())) continue;
            if (file.startsWith("index.")) {
                result.add(file);
                continue;
            }
            if (tpmql != null) {
                var si = mapper.toJsonNode(storage.getRepository().readFile("scenario", file));
                if ((boolean) parser.evaluate(tpmql, si)) {
                    result.add(file);
                }
            } else {
                result.add(file);
            }
        }
        resolversFactory.render("generic/record_plugin/list.jte", result, response);
    }

}
