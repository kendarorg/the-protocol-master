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
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.kendar.apis.ApiUtils.*;

@HttpTypeFilter(hostAddress = "*")
public class ApiStorageHandler implements FilteringClass {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalSettings settings;
    private final ConcurrentLinkedQueue<ProtocolInstance> instances = new ConcurrentLinkedQueue<>();
    private final List<GlobalPluginDescriptor> globalPlugins = new ArrayList<>();

    public ApiStorageHandler(GlobalSettings settings) {
        this.settings = settings;
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
        StorageRepository storage = settings.getService("storage");

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
        StorageRepository storage = settings.getService("storage");

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
        StorageRepository storage = settings.getService("storage");
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
            respondKo(resp, ex);
        }
        return true;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage/items",
            method = "GET", id = "GET /api/global/storage/items")
    @TpmDoc(
            description = "List all data",
            query = @QueryString(
                    key = "maxLength",
                    description = "Max length of tags. Default 100, -1 means no trim",
                    example = "100",
                    type = "integer"),
            responses = {
                    @TpmResponse(
                            body = CompactLineApi[].class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean getItems(Request reqp, Response resp) {
        StorageRepository storage = settings.getService("storage");

        try {
            var maxLengthStr = reqp.getQuery("maxLength");
            if (maxLengthStr == null) {
                maxLengthStr = "100";
            }
            var data = storage.getAllIndexes(Integer.parseInt(maxLengthStr));
            var result = new ArrayList<CompactLineApi>();
            data.sort(Comparator.comparingLong(CompactLine::getIndex));
            for(var item:data){
                var api = mapper.deserialize(mapper.serialize(item), CompactLineApi.class);
                if(api.getFullItemId()!=null && !api.getFullItemId().isEmpty()) {
                    var itemId = reqp.buildUrlNoQuery() + "/" + api.getFullItemId();
                    api.setFullItemAddress(itemId);
                }
                result.add(api);
            }
            respondJson(resp, result);
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/storage/items/{protocol}/{index}",
            method = "GET", id = "GET /api/global/storage/items/{protocol}/{index}")
    @TpmDoc(
            description = "List all data",
            path = {
                    @PathParameter(key = "protocol", description = "The protocol instance id"),
                    @PathParameter(key = "index", description = "The index of the recording")
            },
            responses = {
                    @TpmResponse(
                            body = StorageItem.class
                    ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public boolean getSingleItem(Request reqp, Response resp) {
        StorageRepository storage = settings.getService("storage");

        try {
            var instanceId = reqp.getPathParameter("protocol");
            var itemId = Long.parseLong(reqp.getPathParameter("index"));
            var result = storage.readById(instanceId,itemId);
            respondJson(resp, result);
        } catch (Exception ex) {
            respondKo(resp, ex);
        }
        return true;
    }
}