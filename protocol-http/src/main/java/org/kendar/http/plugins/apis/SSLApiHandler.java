package org.kendar.http.plugins.apis;

import com.fasterxml.jackson.databind.node.TextNode;
import gg.jte.output.StringOutput;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.QueryString;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.events.EventsQueue;
import org.kendar.http.HttpProtocolSettings;
import org.kendar.http.events.SSLAddHostEvent;
import org.kendar.http.events.SSLRemoveHostEvent;
import org.kendar.http.plugins.SSLDummyPlugin;
import org.kendar.http.plugins.settings.SSLDummyPluginSettings;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandler;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.ui.dto.SinglePluginDto;
import org.kendar.utils.FileResourcesUtils;
import org.kendar.utils.JsonMapper;

import static org.kendar.apis.ApiUtils.*;

@HttpTypeFilter()
public class SSLApiHandler implements ProtocolPluginApiHandler {
    private final HttpProtocolSettings protocolSettings;
    private final MultiTemplateEngine resolversFactory;
    private final SSLDummyPlugin descriptor;
    private final String id;
    private final String instanceId;

    public SSLApiHandler(SSLDummyPlugin descriptor, String id, String instanceId,
                         HttpProtocolSettings protocolSettings, MultiTemplateEngine resolversFactory) {
        this.descriptor = descriptor;
        this.id = id;
        this.instanceId = instanceId;
        this.protocolSettings = protocolSettings;
        this.resolversFactory = resolversFactory;
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



    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts")
    @TpmDoc(
            description = "Retrieve the hosts",
            responses = @TpmResponse(
                    body = String[].class,
                    description = "Retrieve the hosts with certificates"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/ssl-plugin"})
    public void retrieveHosts(Request reqp, Response resp) {
        respondJson(resp,protocolSettings.getSSL().getHosts());
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts/")
    @TpmDoc(
            description = "Add host",
            query = @QueryString(key = "host",description = "Host to add" ),
            responses = @TpmResponse(
                    body = Ok.class
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/ssl-plugin"})
    public void addHost(Request reqp, Response resp) {
        var host = reqp.getQuery("host");
        EventsQueue.send(new SSLAddHostEvent(host));
        protocolSettings.getSSL().getHosts().add(host);
        respondJson(resp,protocolSettings.getSSL().getHosts());
    }


    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts")
    @TpmDoc(
            description = "Del host",
            query = @QueryString(key = "host",description = "Host to del" ),
            responses = @TpmResponse(
                    body = Ok.class
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/ssl-plugin"})
    public void delHost(Request reqp, Response resp) {
        var host = reqp.getQuery("host");
        EventsQueue.send(new SSLRemoveHostEvent(host));
        protocolSettings.getSSL().getHosts().remove(host);
        respondJson(resp,protocolSettings.getSSL().getHosts());
    }

    @HttpMethodFilter(
            pathAddress = "/protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts",
            method = "GET", id = "GET /protocols/{#protocolInstanceId}/plugins/{#plugin}/hosts")
    @TpmDoc(
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/ssl-plugin/hosts"})
    public void retrieveHostsPage(Request request, Response response) {

        var model = new SinglePluginDto();
        model.setId(getPluginId());
        model.setInstanceId(instanceId);
        model.setProtocol(protocolSettings.getProtocol());
        model.setActive(true);
        var sets = new SSLDummyPluginSettings();
        model.setSettings(new JsonMapper().serializePretty(sets));
        model.setSettingsObject(sets);
        var output = new StringOutput();
        resolversFactory.render("http/ssl_plugin/hosts.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }
}
