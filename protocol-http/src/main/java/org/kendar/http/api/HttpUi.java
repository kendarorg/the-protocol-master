package org.kendar.http.api;

import com.fasterxml.jackson.databind.node.TextNode;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.http.HttpProtocol;
import org.kendar.plugins.base.ProtocolApiHandler;

@HttpTypeFilter(hostAddress = "*")
public class HttpUi implements ProtocolApiHandler {
    private final HttpProtocol httpProtocol;
    private final TemplateEngine templateEngine;
    private final String protocolInstanceId;

    public HttpUi(HttpProtocol httpProtocol, TemplateEngine templateEngine, String protocolInstanceId) {
        this.httpProtocol = httpProtocol;
        this.templateEngine = templateEngine;

        this.protocolInstanceId = protocolInstanceId;
    }
    @Override
    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public String getId() {
        return getProtocol()+"."+getProtocolInstanceId()+".HttpUi";
    }

    @HttpMethodFilter(
            pathAddress = "/ui/protocols/{#protocolInstanceId}",
            method = "GET", id = "GET /ui/protocols/{#protocolInstanceId}")
    public boolean actionOnAllPlugins(Request reqp, Response resp) {
        var output = new StringOutput();
        var protocols = httpProtocol.getPlugins().stream().map(p->p.getId()).toList();
        var pll = new ProtocolIndex();
        pll.setProtocol(getProtocol());
        pll.setProtocols(protocols);
        pll.setInstanceId(getProtocolInstanceId());

        templateEngine.render("index.jte", pll, output);
        resp.setStatusCode(200);
        resp.addHeader("Content-Type", "text/html");
        resp.setResponseText(TextNode.valueOf(output.toString()));
        return true;
    }
}
