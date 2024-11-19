package org.kendar.http.plugins;

import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.plugins.ProtocolPhase;
import org.kendar.plugins.RewritePlugin;
import org.kendar.utils.ReplacerItemInstance;

import java.net.URL;
import java.util.List;

public class HttpRewritePlugin extends RewritePlugin<Request, Response, String> {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.CONNECT);
    }

    @Override
    protected boolean useTrailing() {
        return true;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    protected String prepare(Request source, Response response) {
        return source.getProtocol() + "://" + source.getHost() + source.getPath();
    }

    @Override
    protected void replaceData(ReplacerItemInstance item, String realSrc, Request source, Response response) {
        try {
            var replaced = item.run(realSrc);
            if (!replaced.equalsIgnoreCase(realSrc)) {
                var oriSource = source.copy();
                var url = new URL(replaced);
                if (url.getProtocol().equalsIgnoreCase(HTTPS) && url.getPort() != 443) {
                    source.setPort(url.getPort());
                    source.setProtocol(HTTPS);
                } else if (url.getProtocol().equalsIgnoreCase(HTTP) && url.getPort() != 80) {
                    source.setPort(url.getPort());
                    source.setProtocol(HTTP);
                } else if (url.getProtocol().equalsIgnoreCase(HTTPS) && url.getPort() == 443) {
                    source.setPort(-1);
                    source.setProtocol(HTTPS);
                } else if (url.getProtocol().equalsIgnoreCase(HTTP) && url.getPort() == 80) {
                    source.setPort(-1);
                    source.setProtocol(HTTP);
                }
                source.setHost(url.getHost());
                source.setPath(url.getPath());
                source.addOriginal(oriSource);
                source.setPort(url.getPort());
                source.setProtocol(url.getProtocol());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
