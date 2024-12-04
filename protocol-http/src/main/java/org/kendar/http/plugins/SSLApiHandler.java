package org.kendar.http.plugins;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.kendar.annotations.HamDoc;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.multi.HamResponse;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.utils.FileResourcesUtils;

@HttpTypeFilter(hostAddress = "*")
public class SSLApiHandler extends ProtocolPluginApiHandlerDefault<SSLDummyPlugin> {
    private final HttpProtocolSettings protocolSettings;

    public SSLApiHandler(SSLDummyPlugin descriptor, String id, String instanceId, HttpProtocolSettings protocolSettings) {
        super(descriptor, id, instanceId);
        this.protocolSettings = protocolSettings;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{id}/plugins/ssl-plugin/{action}",
            method = "GET", id = "GET /api/protocols/{id}/plugins/ssl-plugin/{action}")
    @HamDoc(
            description = "Retrieve the root certificates",
            path = @PathParameter(key = "action",
            allowedValues = {"der","key"}),
            responses = @HamResponse(
                    body = byte[].class,
                    content = "application/pkix-crl",
                    description = "Retrieve the root certificates"
            ),
            tags = {"base/utils"})
    public boolean retrieveDerKey(Request reqp, Response resp){
        var action = reqp.getPathParameter("action");
        var protocolInstanceId = reqp.getPathParameter("id");
        if(protocolSettings.getProtocolInstanceId().equals(protocolInstanceId)){
            try {
                var frf = new FileResourcesUtils();
                switch (action) {
                    case "/der":
                        var data = frf.getFileFromResourceAsByteArray(protocolSettings.getSSL().getDer());
                        resp.setResponseText(new BinaryNode(data));
                        resp.addHeader(ConstantsHeader.CONTENT_TYPE, "application/pkix-crl");
                        resp.addHeader("Content-Transfer-Encoding", "binary");
                        resp.addHeader("Content-Disposition", "attachment; filename=\"certificate.der\";");
                        return true;
                    case "/key":
                        var key = frf.getFileFromResourceAsByteArray(protocolSettings.getSSL().getKey());
                        resp.setResponseText(new BinaryNode(key));
                        resp.addHeader(ConstantsHeader.CONTENT_TYPE, "application/pkix-crl");
                        resp.addHeader("Content-Transfer-Encoding", "binary");
                        resp.addHeader("Content-Disposition", "attachment; filename=\"certificate.key\";");
                        return true;
                }
            }catch (Exception ex){

                resp.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
                resp.setResponseText(mapper.toJsonNode(new Ko(ex.getMessage())));
                return true;

            }
        }
        return false;
    }
}
