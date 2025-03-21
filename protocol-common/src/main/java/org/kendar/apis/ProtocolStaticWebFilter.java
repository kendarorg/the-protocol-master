package org.kendar.apis;

import org.kendar.apis.filters.StaticWebFilter;
import org.kendar.exceptions.ApiException;
import org.kendar.plugins.base.ProtocolApiHandler;
import org.kendar.utils.FileResourcesUtils;

import java.io.IOException;

/**
 * Offers an api relative to the Protocol
 * The path on resources should be "*webPROTOCOL"
 */
public abstract class ProtocolStaticWebFilter extends StaticWebFilter implements ProtocolApiHandler {
    private final String protocolInstanceId;

    public ProtocolStaticWebFilter(FileResourcesUtils fileResourcesUtils, String protocolInstanceId) {

        super(fileResourcesUtils);
        this.protocolInstanceId = protocolInstanceId;
        try {
            loadAllStuffs();
        } catch (IOException e) {
            throw new ApiException("Unable to load data for protocol "+protocolInstanceId,e);
        }
    }

    @Override
    public String adaptRequestedPath(String requestedPath) {
        return requestedPath
                .replace("/" + protocolInstanceId, "/" + getProtocol());
    }

    @Override
    public String getId() {
        return getProtocol() + "." + getProtocolInstanceId() + ".web";
    }

    @Override
    protected String getPath() {
        return "*web" + getProtocol();
    }

    @Override
    public boolean isPathMatching(String path) {
        return path.contains("/" + protocolInstanceId);
    }

    @Override
    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }
}
