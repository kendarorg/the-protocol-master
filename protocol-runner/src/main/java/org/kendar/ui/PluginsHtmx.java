package org.kendar.ui;

import com.fasterxml.jackson.databind.node.TextNode;
import gg.jte.output.StringOutput;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.BasePluginDescriptor;
import org.kendar.plugins.base.GlobalPluginDescriptor;
import org.kendar.plugins.base.ProtocolInstance;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.ui.dto.*;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("HttpUrlsUsage")
@TpmService
@HttpTypeFilter(
        blocking = true)
public class PluginsHtmx implements FilteringClass {
    @Override
    public String getId() {
        return this.getClass().getName();
    }

    private final JsonMapper mapper;
    private final MultiTemplateEngine resolversFactory;
    private final DiService diService;

    public PluginsHtmx(JsonMapper mapper, MultiTemplateEngine resolversFactory, DiService diService) {
        this.mapper = mapper;
        this.resolversFactory = resolversFactory;
        this.diService = diService;
    }


    @HttpMethodFilter(
            pathAddress = "/globalpl",
            method = "GET", id = "GET /globalpl")
    public void globalpl(Request request, Response response) {
        var output = new StringOutput();
        var model = new GlobalsDto();
        for(var globalPlugin:diService.getInstances(GlobalPluginDescriptor.class)){
            model.getPlugins().add(convert(null,globalPlugin));
        }
        resolversFactory.render("globalpl.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/plugins",
            method = "GET", id = "GET /plugins")
    public void plugins(Request request, Response response) {
        var output = new StringOutput();
        var model = new ProtocolStatusDto();
        var instances = (List<ProtocolInstance>)diService.getNamedInstance("protocols",new ArrayList<ProtocolInstance>().getClass());
        for(var instance: instances){
            if(instance==null) continue;
            var protocol = (NetworkProtoDescriptor) instance.getServer().getProtoDescriptor();
            model.getProtocols().add(this.convert(protocol));
        }
        resolversFactory.render("plugins.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/plugins/wildcard",
            method = "GET", id = "GET /plugins/wildcard")
    public void pluginsWildcard(Request request, Response response) {
        var avoidScript = "true".equalsIgnoreCase(request.getQuery("avoidScript"));
        var output = new StringOutput();
        var model = new ProtocolStatusDto();
        model.getParameters().put("avoidScript",avoidScript);
        var instances = (List<ProtocolInstance>)diService.getNamedInstance("protocols",new ArrayList<ProtocolInstance>().getClass());
        for(var instance: instances){
            var protocol = (NetworkProtoDescriptor) instance.getServer().getProtoDescriptor();
            model.getProtocols().add(this.convert(protocol));
        }
        resolversFactory.render("plugins/wildcard.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/plugins/active",
            method = "GET", id = "GET /plugins/active")
    public void pluginsActive(Request request, Response response) {
        var avoidScript = "true".equalsIgnoreCase(request.getQuery("avoidScript"));
        var output = new StringOutput();
        var model = new ProtocolStatusDto();
        model.getParameters().put("avoidScript",avoidScript);
        var instances = (List<ProtocolInstance>)diService.getNamedInstance("protocols",new ArrayList<ProtocolInstance>().getClass());
        for(var instance: instances){
            var protocol = (NetworkProtoDescriptor) instance.getServer().getProtoDescriptor();
            model.getProtocols().add(this.convert(protocol));
        }
        resolversFactory.render("plugins/active.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/plugins/protocols",
            method = "GET", id = "GET /plugins/protocols")
    public void pluginsProtocols(Request request, Response response) {
        var avoidScript = "true".equalsIgnoreCase(request.getQuery("avoidScript"));
        var protocolId = request.getQuery("protocolId");
        var output = new StringOutput();
        var instances = (List<ProtocolInstance>)diService.getNamedInstance("protocols",new ArrayList<ProtocolInstance>().getClass());
        for(var instance: instances){
            var protocol = (NetworkProtoDescriptor) instance.getServer().getProtoDescriptor();
            if(protocol.getSettings().getProtocolInstanceId().equals(protocolId)){
                var model = this.convert(protocol);
                model.getParameters().put("avoidScript",avoidScript);
                resolversFactory.render("plugins/protocol.jte",model,output);
                response.addHeader("Content-type","text/html");
                response.setResponseText(new TextNode(output.toString()));
                response.setStatusCode(200);
                return;
            }
        }
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/plugins/{protocolInstanceId}/{pluginId}",
            method = "GET", id = "GET /plugins/{protocolInstanceId}/{pluginId}")
    public void singlePlugin(Request request, Response response) {
        var instanceId = request.getPathParameter("protocolInstanceId");
        var pluginId = request.getPathParameter("pluginId");
        var output = new StringOutput();
        var model = new SinglePluginDto();
        if(!instanceId.equalsIgnoreCase("global")){
            var instances = (List<ProtocolInstance>)diService.getNamedInstance("protocols",new ArrayList<ProtocolInstance>().getClass());
            var instance = instances.stream().filter(i->i.getProtocolInstanceId().equalsIgnoreCase(instanceId)).findFirst().orElse(null);
            var plugin= instance.getPlugins().stream().filter(i->i.getId().equalsIgnoreCase(pluginId)).findFirst().orElse(null);
            model.setId(plugin.getId());
            model.setInstanceId(instanceId);
            model.setProtocol(plugin.getProtocol());
            model.setActive(plugin.isActive());
            model.setSettings(mapper.serializePretty(plugin.getSettings()));
            resolversFactory.render("plugins/single.jte",model,output);
        }else{
            var plugin=(GlobalPluginDescriptor)diService.getInstances(GlobalPluginDescriptor.class)
                    .stream().filter(i->i.getId().equalsIgnoreCase(pluginId)).findFirst().orElse(null);
            model.setId(plugin.getId());
            model.setInstanceId("global");
            model.setProtocol("global");
            model.setActive(plugin.isActive());
            model.setSettings(mapper.serializePretty(plugin.getSettings()));
            resolversFactory.render("plugins/singlegl.jte",model,output);
        }

        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    private ProtocolDto convert(NetworkProtoDescriptor protocol) {
        var dto = new ProtocolDto();
        dto.setInstanceId(protocol.getSettings().getProtocolInstanceId());
        dto.setProtocol(protocol.getSettings().getProtocol());
        for(var port:protocol.getPorts().entrySet()){
            if(port.getValue()>0){
                dto.getOpenPorts().put(port.getKey(),port.getValue());
            }
        }
        for(var plugin:protocol.getPlugins()){
            dto.getPlugins().add(this.convert(dto,plugin));
        }
        return dto;
    }

    private PluginDto convert(ProtocolDto protocol, BasePluginDescriptor plugin) {
        var dto = new PluginDto();
        if(protocol!=null) {
            dto.setInstanceId(protocol.getInstanceId());
            dto.setProtocol(protocol.getProtocol());
        }
        dto.setId(plugin.getId());
        dto.setActive(plugin.isActive());
        return dto;
    }
}
