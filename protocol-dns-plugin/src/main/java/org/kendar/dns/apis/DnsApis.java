package org.kendar.dns.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.dns.DnsMapping;
import org.kendar.dns.DnsProtocol;
import org.kendar.dns.DnsProtocolSettings;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolApiHandler;
import org.kendar.utils.JsonMapper;

import java.util.List;

import static org.kendar.apis.ApiUtils.respondJson;

@HttpTypeFilter(blocking = true)
public class DnsApis implements ProtocolApiHandler {
    private final JsonMapper mapper;
    private final DnsProtocol protocol;
    private final DnsProtocolSettings settings;

    public DnsApis(JsonMapper mapper, DnsProtocol protocol, DnsProtocolSettings settings) {
        this.mapper = mapper;
        this.protocol = protocol;
        this.settings = settings;
    }

    @Override
    public String getProtocolInstanceId() {
        return settings.getProtocolInstanceId();
    }

    @Override
    public String getProtocol() {
        return "dns";
    }

    @Override
    public String getId() {
        return settings.getProtocolInstanceId() + "_" + this.getClass().getSimpleName();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/dns/registered",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/dns/registered")
    @TpmDoc(
            description = "Add/update dns registration",
            requests = @TpmRequest(body = DnsMapping[].class),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/dns/{#protocolInstanceId}"})
    public void updateDnsRegistered(Request reqp, Response resp) {
        var dataList = mapper.deserialize(reqp.getRequestText().toString(), DnsMapping[].class);
        for(var data:dataList) {
            var somethingChanged = false;
            for (var setting : settings.getRegistered()) {
                if (setting.getName().equals(data.getName())) {
                    setting.setIp(data.getIp());
                    somethingChanged = true;
                    break;
                }
            }
            if (!somethingChanged) {
                settings.getRegistered().add(data);
            }
        }
        protocol.clearCache();

    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/dns/registered/{dnsName}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/dns/registered/{dnsName}")
    @TpmDoc(
            description = "Delete dns registration",
            path = @PathParameter(key = "dnsName", description = "Dns name to remove"),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/dns/{#protocolInstanceId}"})
    public void deleteDnsRegistered(Request reqp, Response resp) {
        var dnsName = reqp.getPathParameter("dnsName");
        var somethingChanged = false;
        List<DnsMapping> registered = settings.getRegistered();
        for (int i = registered.size() - 1; i >= 0; i--) {
            var setting = registered.get(i);
            if (setting.getName().equalsIgnoreCase(dnsName)||setting.getIp().equalsIgnoreCase(dnsName)) {
                settings.getRegistered().remove(i);
                somethingChanged = true;
                break;
            }
        }
        if (somethingChanged) {
            protocol.clearCache();
        }
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/dns/registered",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/dns/registered")
    @TpmDoc(
            description = "Get dns registration",
            responses = {@TpmResponse(
                    body = DnsMapping[].class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/dns/{#protocolInstanceId}"})
    public void getDnsRegistered(Request reqp, Response resp) {
        respondJson(resp,settings.getRegistered());
    }



    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/dns/blocked",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/dns/blocked")
    @TpmDoc(
            description = "Get dns blocked",
            responses = {@TpmResponse(
                    body = String[].class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/dns/{#protocolInstanceId}"})
    public void getDnsBlocked(Request reqp, Response resp) {
        respondJson(resp,settings.getBlocked());
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/dns/blocked",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/dns/blocked")
    @TpmDoc(
            description = "Add/update dns blocked dns",
            requests = @TpmRequest(body = String[].class),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/dns/{#protocolInstanceId}"})
    public void updateDnsBlocked(Request reqp, Response resp) {

        var dataList = mapper.deserialize(reqp.getRequestText().toString(), String[].class);
        for(var dnsName:dataList) {
            var alreadyPresent = false;
            for (var setting : settings.getBlocked()) {
                if (setting.equalsIgnoreCase(dnsName)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if(!alreadyPresent){
                settings.getBlocked().add(dnsName);
            }
        }
        protocol.clearCache();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/dns/blocked/{dnsName}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/dns/blocked/{dnsName}")
    @TpmDoc(
            description = "Delete dns blocked",
            path = @PathParameter(key = "dnsName", description = "Dns name to remove"),
            responses = {@TpmResponse(
                    body = Ok.class
            ), @TpmResponse(
                    body = Ko.class
            )},
            tags = {"plugins/dns/{#protocolInstanceId}"})
    public void deleteDnsBlocked(Request reqp, Response resp) {
        var dnsName = reqp.getPathParameter("dnsName");
        var somethingChanged = false;
        List<String> registered = settings.getBlocked();
        for (int i = 0; i < registered.size(); i++) {
            var setting = registered.get(i);
            if (setting.equalsIgnoreCase(dnsName)) {
                settings.getBlocked().remove(i);
                somethingChanged = true;
                break;
            }
        }
        if (somethingChanged) {
            protocol.clearCache();
        }
    }
}
