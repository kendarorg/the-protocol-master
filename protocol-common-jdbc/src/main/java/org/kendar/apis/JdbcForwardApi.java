package org.kendar.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.DiService;
import org.kendar.plugins.BasicJdbcForwardPlugin;
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
    private final PluginFileManager storage;
    private final MultiTemplateEngine resolversFactory;

    public JdbcForwardApi(BasicJdbcForwardPlugin descriptor, String id, String instanceId,
                          PluginFileManager storage, MultiTemplateEngine resolversFactory) {
        super(descriptor, id, instanceId);
        this.storage = storage;
        this.resolversFactory = resolversFactory;
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward"})
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward"})
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
            tags = {"plugins/{#protocol}/{#protocolInstanceId}/jdbc-forward"})
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
}