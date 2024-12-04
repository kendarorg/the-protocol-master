package org.kendar.http.plugins;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.utils.FileResourcesUtils;

import static org.kendar.apis.ApiUtils.respondFile;
import static org.kendar.apis.ApiUtils.respondKo;

@HttpTypeFilter(hostAddress = "*")
public class SSLApiHandler extends ProtocolPluginApiHandlerDefault<SSLDummyPlugin> {
    private final HttpProtocolSettings protocolSettings;

    public SSLApiHandler(SSLDummyPlugin descriptor, String id, String instanceId, HttpProtocolSettings protocolSettings) {
        super(descriptor, id, instanceId);
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}"})
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
}
