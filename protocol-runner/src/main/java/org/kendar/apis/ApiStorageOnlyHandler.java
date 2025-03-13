package org.kendar.apis;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.QueryString;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.dtos.CompactLineApi;
import org.kendar.apis.dtos.StorageAndIndex;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.kendar.apis.ApiUtils.*;

@TpmService
@HttpTypeFilter()
public class ApiStorageOnlyHandler implements FilteringClass {
    private static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(ApiStorageOnlyHandler.class);
    private final GlobalSettings settings;
    private final StorageRepository storage;
    private final SimpleParser simpleParser;
    private final ConcurrentLinkedQueue<ProtocolInstance> instances = new ConcurrentLinkedQueue<>();
    private final List<GlobalPluginDescriptor> globalPlugins = new ArrayList<>();


    public ApiStorageOnlyHandler(GlobalSettings settings, StorageRepository storage, SimpleParser simpleParser) {
        this.settings = settings;
        this.storage = storage;
        this.simpleParser = simpleParser;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage",
            method = "GET", id = "GET /api/global/storage")
    @TpmDoc(
            description = "Download recorded data",
            responses = {@TpmResponse(
                    content = ConstantsMime.ZIP,
                    body = byte[].class
            ), @TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean handleDownload(Request reqp, Response resp) {
        try {
            var data = storage.readAsZip();
            respondFile(resp, data, ConstantsMime.ZIP, "storage.zip");
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage",
            method = "DELETE", id = "DELETE /api/global/storage")
    @TpmDoc(
            description = "Delete recorded data",
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean cleanUp(Request reqp, Response resp) {
        try {
            storage.clean();
            respondOk(resp);
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage",
            method = "POST", id = "POST /api/global/storage")
    @TpmDoc(
            description = "Upload existing recording can call with " +
                    "<a href='upload.html?path=/api/global/storage&contentType=application/octet-stream&binary=true'>Upload</a>",
            requests = @TpmRequest(
                    accept = ConstantsMime.ZIP
            ),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean handleUpload(Request reqp, Response resp) {

        try {

            byte[] inputData;
            if (reqp.getRequestText() instanceof BinaryNode) {
                inputData = ((BinaryNode) reqp.getRequestText()).binaryValue();
            } else {
                inputData = reqp.getRequestText().textValue().getBytes();
            }
            storage.writeZip(inputData);
            storage.initialize();
            respondOk(resp);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            respondKo(resp, ex);
        }
        return true;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage/index",
            method = "GET", id = "GET /api/global/storage/index")
    @TpmDoc(
            description = "List all recorded indexes<br>Uses <a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> query language",
            query = {@QueryString(
                    key = "maxLength",
                    description = "Max length of tags. Default 100, -1 means no trim",
                    example = "100",
                    type = "integer"),
                    @QueryString(
                            key = "tpmql",
                            description = "<a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> selection query",
                            example = "")},
            responses = {
                    @TpmResponse(
                            body = CompactLineApi[].class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean getIndexs(Request reqp, Response resp) {

        var tpmqlstring = reqp.getQuery("tpmql");
        Token tpmql = null;
        if (tpmqlstring != null && !tpmqlstring.isEmpty()) {
            tpmql = simpleParser.parse(tpmqlstring);
        }
        try {
            var maxLengthStr = reqp.getQuery("maxLength");
            if (maxLengthStr == null) {
                maxLengthStr = "100";
            }
            var data = storage.getAllIndexes(Integer.parseInt(maxLengthStr));
            var result = new ArrayList<CompactLineApi>();
            data.sort(Comparator.comparingLong(CompactLine::getIndex));
            for (var item : data) {
                var api = mapper.deserialize(mapper.serialize(item), CompactLineApi.class);
                if (api.getFullItemId() != null && !api.getFullItemId().isEmpty()) {
                    var itemId = reqp.buildUrlNoQuery().replace("/index", "/item") + "/" + api.getFullItemId();
                    api.setFullItemAddress(itemId);
                }
                if (tpmql != null) {
                    var toEvaluate = mapper.toJsonNode(api);
                    if ((boolean) simpleParser.evaluate(tpmql, toEvaluate)) {
                        result.add(api);
                    }
                } else {
                    result.add(api);
                }
            }
            respondJson(resp, result);
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage/item",
            method = "GET", id = "GET /api/global/storage/item")
    @TpmDoc(
            description = "List all recorded items<br>Uses <a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> query language",
            query = {@QueryString(
                    key = "tpmql",
                    description = "<a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> selection query",
                    example = "")},
            responses = {
                    @TpmResponse(
                            body = StorageAndIndex[].class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean getItems(Request reqp, Response resp) {

        var tpmqlstring = reqp.getQuery("tpmql");
        Token tpmql = null;
        if (tpmqlstring != null && !tpmqlstring.isEmpty()) {
            tpmql = simpleParser.parse(tpmqlstring);
        }
        var result = new ArrayList<StorageAndIndex>();
        try {
            var sai = new StorageAndIndex();

            for (var optIndex : storage.getAllIndexes(-1)) {
                var fullText = storage.readFromScenarioById(optIndex.getProtocolInstanceId(), optIndex.getIndex());
                if (fullText != null) {
                    sai.setItem(fullText);
                }
                var api = mapper.deserialize(mapper.serialize(optIndex), CompactLineApi.class);
                if (api.getFullItemId() != null && !api.getFullItemId().isEmpty()) {
                    var theItemId = reqp.buildUrlNoQuery() + "/" + api.getFullItemId();
                    api.setFullItemAddress(theItemId);
                }
                sai.setIndex(api);

                if (tpmql != null) {
                    var toEvaluate = mapper.toJsonNode(sai);
                    if ((boolean) simpleParser.evaluate(tpmql, toEvaluate)) {
                        result.add(sai);
                    }
                } else {
                    result.add(sai);
                }
            }
            respondJson(resp, result);

            return true;
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage/item/{protocol}/{index}",
            method = "GET", id = "GET /api/global/storage/item/{protocol}/{index}")
    @TpmDoc(
            description = "List single item",
            path = {
                    @PathParameter(key = "protocol", description = "The protocol instance id"),
                    @PathParameter(key = "index", description = "The index of the recording")
            },
            responses = {
                    @TpmResponse(
                            body = StorageAndIndex.class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean getSingleItem(Request reqp, Response resp) {

        var result = new StorageAndIndex();

        try {
            var instanceId = reqp.getPathParameter("protocol");
            var itemId = Long.parseLong(reqp.getPathParameter("index"));
            result.setItem(storage.readFromScenarioById(instanceId, itemId));
            var optIndex = storage.getAllIndexes(-1).stream().filter(a -> a.getIndex() == itemId).findFirst();
            if (optIndex.isPresent()) {
                var api = mapper.deserialize(mapper.serialize(optIndex.get()), CompactLineApi.class);
                if (api.getFullItemId() != null && !api.getFullItemId().isEmpty()) {
                    var theItemId = reqp.buildUrlNoQuery() + "/" + api.getFullItemId();
                    api.setFullItemAddress(theItemId);
                }
                result.setIndex(api);
                respondJson(resp, result);
            }
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage/items/{protocol}/{index}",
            method = "PUT", id = "PUT /api/global/storage/items/{protocol}/{index}")
    @TpmDoc(
            description = "Change single item",
            path = {
                    @PathParameter(key = "protocol", description = "The protocol instance id"),
                    @PathParameter(key = "index", description = "The index of the recording")
            },
            requests = @TpmRequest(
                    body = StorageAndIndex.class
            ),
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean changeSingleItem(Request reqp, Response resp) {

        var request = mapper.deserialize(reqp.getRequestText().toString(), StorageAndIndex.class);

        try {
            var instanceId = reqp.getPathParameter("protocol");
            var itemId = Long.parseLong(reqp.getPathParameter("index"));
            var optIndex = storage.getAllIndexes(-1).stream().filter(a -> a.getIndex() == itemId).findFirst();
            if (optIndex.isPresent()) {
                var indexItem = optIndex.get();
                if (request.getItem() != null) {
                    request.getItem().setIndex(itemId);
                }
                request.getIndex().setIndex(itemId);
                storage.updateRecording(itemId, indexItem.getProtocolInstanceId(), request.getIndex(), request.getItem());
                respondJson(resp, new Ok());
            }
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }


    @HttpMethodFilter(
            pathAddress = "/api/global/storage/items/{protocol}/{index}",
            method = "DELETE", id = "DELETE /api/global/storage/items/{protocol}/{index}")
    @TpmDoc(
            description = "Delete single item",
            path = {
                    @PathParameter(key = "protocol", description = "The protocol instance id"),
                    @PathParameter(key = "index", description = "The index of the recording")
            },
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean deleteSingleItem(Request reqp, Response resp) {


        try {
            var instanceId = reqp.getPathParameter("protocol");
            var itemId = Long.parseLong(reqp.getPathParameter("index"));
            storage.deleteRecording(instanceId, itemId);
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }
}