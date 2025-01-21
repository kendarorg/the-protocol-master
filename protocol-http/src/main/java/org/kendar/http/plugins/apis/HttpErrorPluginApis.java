package org.kendar.http.plugins.apis;

import com.fasterxml.jackson.databind.node.TextNode;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.http.HttpProtocol;
import org.kendar.http.plugins.HttpErrorPlugin;
import org.kendar.http.plugins.HttpErrorPluginSettings;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;

@HttpTypeFilter(hostAddress = "*")
public class HttpErrorPluginApis extends ProtocolPluginApiHandlerDefault<HttpErrorPlugin> {

    private final TemplateEngine templateEngine;

    public HttpErrorPluginApis(HttpErrorPlugin descriptor, String id, String instanceId) {
        super(descriptor, id, instanceId);
        templateEngine = ((HttpProtocol)descriptor.getProtocolInstance()).getTemplateEngine();

    }

    @HttpMethodFilter(
            pathAddress = "/ui/protocols/{#protocolInstanceId}/plugins/{#plugin}",
            method = "GET", id = "GET /ui/protocols/{#protocolInstanceId}/plugins/{#plugin}")
    public boolean actionOnAllPlugins(Request reqp, Response resp) {
        var output = new StringOutput();

        templateEngine.render("error-plugin/index.jte", getDescriptor().getSettings(), output);
        resp.setStatusCode(200);
        resp.addHeader("Content-Type", "text/html");
        resp.setResponseText(TextNode.valueOf(output.toString()));
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/ui/protocols/{#protocolInstanceId}/plugins/{#plugin}",
            method = "POST", id = "POST /ui/protocols/{#protocolInstanceId}/plugins/{#plugin}")
    public boolean saveData(Request reqp, Response resp) {
        var output = new StringOutput();
        var setts = mapper.deserialize(reqp.getRequestText().toString(),HttpErrorPluginSettings.class);
        getDescriptor().getSettings().setErrorMessage(setts.getErrorMessage());
        getDescriptor().getSettings().setErrorPercent(setts.getErrorPercent());
        getDescriptor().getSettings().setShowError(setts.getShowError());

        templateEngine.render("error-plugin/data.jte", getDescriptor().getSettings(), output);
        resp.setStatusCode(200);
        resp.addHeader("Content-Type", "text/html");
        resp.setResponseText(TextNode.valueOf(output.toString()));
        return true;
    }
}
