package org.kendar.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.QueryString;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.DiService;
import org.kendar.plugins.BasicJdbcForwardPlugin;
import org.kendar.plugins.JdbcForwardMatcher;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.plugins.base.ProtocolPluginApiHandlerDefault;
import org.kendar.apis.dto.JdbcForwardDto;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.PluginFileManager;
import org.kendar.ui.MultiTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@HttpTypeFilter()
public class JdbcForwardApi extends ProtocolPluginApiHandlerDefault<BasicJdbcForwardPlugin> {

    public JdbcForwardApi(BasicJdbcForwardPlugin descriptor, String id, String instanceId) {
        super(descriptor, id, instanceId);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/forwards",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/forwards")
    @TpmDoc(
            description = "Retrieve all the forwards",
            responses = @TpmResponse(
                    body = JdbcForwardDto[].class,
                    description = "Retrieve all the forwards"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward-plugin"})
    public boolean listAllForwardsd(Request reqp, Response resp) {
        var result = getDescriptor().getMatchers().stream().map(m -> new JdbcForwardDto(m.getId(),m.getOriSource(), m.getOriTarget())).toList();
        respondJson(resp, result);
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/forwards/{id}",
            method = "POST", id = "POST /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/forwards/{id}")
    @TpmDoc(
            description = "Update/insert the forwarder",
            path = {@PathParameter(key = "id")},
            requests = @TpmRequest(body = JdbcForwardDto.class),
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward-plugin"})
    public void putSingleForward(Request reqp, Response resp) {
        var pluginInstance = getDescriptor();
        var id = reqp.getPathParameter("id");
        var inputData = reqp.getRequestText().toString();
        var data = mapper.deserialize(inputData, JdbcForwardDto.class);
        var forwards = new ArrayList<>(getDescriptor().getMatchers().stream().map(m -> new JdbcForwardDto(m.getId(), m.getOriSource(), m.getOriTarget())).toList());
        var matchingId = forwards.stream().filter(m -> (
                data.getSource().equalsIgnoreCase(m.getSource())||
                data.getTarget().equalsIgnoreCase(m.getTarget())||
         m.getId().equalsIgnoreCase(id))).findFirst();

        var settings = pluginInstance.getSettings();
        var ppdb = (BasicJdbcForwardPlugin) pluginInstance;

        if (matchingId.isPresent()) {
            matchingId.get().setSource(data.getSource());
            matchingId.get().setTarget(data.getTarget());
        } else {
            forwards.add(new JdbcForwardDto("", data.getSource(), data.getTarget()));
        }
        var newData = new HashMap<String, String>();
        forwards.forEach(m -> newData.put(m.getSource(), m.getTarget()));
        settings.setMappings(newData);

        var globalSettings = DiService.getThreadContext().getInstance(GlobalSettings.class);
        var pfk = globalSettings.getProtocolForKey(ppdb.getInstanceId());
        pfk.getPlugins().put(pluginInstance.getId(), settings);
        ppdb.setSettings(settings);
        respondOk(resp);
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/forwards/{id}",
            method = "DELETE", id = "DELETE /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/forwards/{id}")
    @TpmDoc(
            description = "Remove the specific forward",
            path = {@PathParameter(key = "id")},
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward-plugin"})
    public void delSingleForward(Request reqp, Response resp) {
        var pluginInstance = getDescriptor();
        var id = reqp.getPathParameter("id");

        var forwards = new ArrayList<>(getDescriptor().getMatchers().stream().map(m -> new JdbcForwardDto(m.getId(), m.getOriSource(), m.getOriTarget())).toList());
        var newData = new HashMap<String,String>();
        forwards.forEach(m->{
            if(!m.getId().equalsIgnoreCase(id)){
                newData.put(m.getSource(),m.getTarget());
            }
        });

        var settings = pluginInstance.getSettings();
        var ppdb = (BasicJdbcForwardPlugin) pluginInstance;
        settings.setMappings(newData);

        var globalSettings = DiService.getThreadContext().getInstance(GlobalSettings.class);
        var pfk = globalSettings.getProtocolForKey(ppdb.getInstanceId());
        pfk.getPlugins().put(pluginInstance.getId(), settings);
        ppdb.setSettings(settings);
        respondOk(resp);
    }


    @HttpMethodFilter(
            pathAddress = "/api/protocols/{#protocolInstanceId}/plugins/{#plugin}/test",
            method = "GET", id = "GET /api/protocols/{#protocolInstanceId}/plugins/{#plugin}/test")
    @TpmDoc(
            description = "TestParamsMatching",
            query = {
                    @QueryString(key="source", description = "Input to test"),
                    @QueryString(key="target", description = "Replace to test"),
                    @QueryString(key="data", description = "What to test")
},
            responses = @TpmResponse(
                    body = String.class,
                    description = "Test the current forward"
            ),
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward-plugin"})
    public boolean testParams(Request reqp, Response resp) {
        var source = reqp.getQuery("source");
        var target = reqp.getQuery("target");
        var data = reqp.getQuery("data");
        var parse = new JdbcForwardMatcher(source, target);
        var result = parse.match(data);
        respondJson(resp, result);
        return true;
    }
}