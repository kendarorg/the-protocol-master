package org.kendar.proxy;

import java.net.URI;
import java.net.URISyntaxException;

public class ProxyRequest {

    private final String verb;
    private final String protocol;
    private String path;
    private int port;
    private String host;

    public ProxyRequest(String request) throws URISyntaxException {


        var res = request.split(" ");
        verb = res[0];
        var content = res[1];
        protocol = res[2];
        if (isConnect()) {
            var spl = content.split(":");
            host = spl[0];
            port = 80;
            if (spl.length > 1) {
                port = Integer.parseInt(spl[1]);
            }
        } else {
            var uri = new URI(content);
            host = uri.getHost();
            path = uri.getPath();
            if (uri.getScheme().equals("https")) {
                port = 443;
            } else if (uri.getScheme().equals("http")) {
                port = 80;
            }
            if (uri.getPort() > 0) {
                port = uri.getPort();
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getVerb() {
        return verb;
    }

    public boolean isConnect() {
        return verb.equalsIgnoreCase("connect");
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }
}
