package org.kendar.http.plugins;

import com.sun.net.httpserver.HttpExchange;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.plugins.BaseApiServerHandler;
import org.kendar.plugins.DefaultPluginApiHandler;
import org.kendar.plugins.apis.FileDownload;
import org.kendar.utils.FileResourcesUtils;

public class SSLApiHandler extends DefaultPluginApiHandler<SSLDummyPlugin> {
    private final HttpProtocolSettings protocolSettings;

    public SSLApiHandler(SSLDummyPlugin descriptor, String id, String instanceId, HttpProtocolSettings protocolSettings) {
        super(descriptor, id, instanceId);
        this.protocolSettings = protocolSettings;
    }

    @Override
    public boolean handle(BaseApiServerHandler apiServerHandler, HttpExchange exchange, String pathPart) {
        try {
            var frf = new FileResourcesUtils();
            switch (pathPart) {
                case "/der":
                    var data = frf.getFileFromResourceAsByteArray(protocolSettings.getSSL().getDer());
                    apiServerHandler.respond(exchange, new FileDownload(data, "certificate.der", "application/pkix-crl"), 200);
                    return true;
                case "/key":
                    var key = frf.getFileFromResourceAsByteArray(protocolSettings.getSSL().getKey());
                    apiServerHandler.respond(exchange, new FileDownload(key, "certificate.key", "application/pkix-crl"), 200);
                    return true;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }
}
