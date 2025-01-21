package org.kendar.http;


import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.ProtocolStaticWebFilter;
import org.kendar.utils.FileResourcesUtils;

@HttpTypeFilter(hostAddress = "*")
public class HttpWebSite extends ProtocolStaticWebFilter {

    public HttpWebSite(FileResourcesUtils fileResourcesUtils, String protocolInstanceId) {
        super(fileResourcesUtils, protocolInstanceId);
    }



    @Override
    public String getProtocol() {
        return "http";
    }
}
