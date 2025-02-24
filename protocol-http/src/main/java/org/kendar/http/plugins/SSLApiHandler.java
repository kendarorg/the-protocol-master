package org.kendar.http.plugins;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.http.HttpProtocolSettings;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.utils.FileResourcesUtils;

import static org.kendar.apis.ApiUtils.respondFile;
import static org.kendar.apis.ApiUtils.respondKo;

@HttpTypeFilter()
public class SSLApiHandler implements ProtocolPluginApiHandler {
    private final HttpProtocolSettings protocolSettings;
    private final SSLDummyPlugin descriptor;
    private final String id;
    private final String instanceId;

    public SSLApiHandler(SSLDummyPlugin descriptor, String id, String instanceId, HttpProtocolSettings protocolSettings) {
        this.descriptor = descriptor;
        this.id = id;
        this.instanceId = instanceId;
        this.protocolSettings = protocolSettings;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/{action}",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/{action}")
    @TpmDoc(
            description = "Retrieve the root certificates",
            path = {@PathParameter(key = "action",
                    allowedValues = {"der", "key"})},
            responses = @TpmResponse(
                    body = byte[].class,
                    content = "application/pkix-crl",
                    description = "Retrieve the root certificates"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/ssl-plugin"})
    public boolean retrieveDerKey(Request reqp, Response resp) {
        var action = reqp.getPathParameter("action");

        try {
            var frf = new FileResourcesUtils();
            switch (action) {
                case "der":
                    var data = frf.getFileFromResourceAsByteArray(protocolSettings.getSSL().getDer());
                    respondFile(resp, data, "application/pkix-crl", "certificate.der");
                    return true;
                case "key":
                    var key = frf.getFileFromResourceAsByteArray(protocolSettings.getSSL().getKey());
                    respondFile(resp, key, "application/pkix-crl", "certificate.key");
                    return true;
            }
        } catch (Exception ex) {
            respondKo(resp, ex);
            return true;

        }
        return false;
    }

    public SSLDummyPlugin getDescriptor() {
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
}
