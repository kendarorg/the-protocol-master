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
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.dto.FileTreeItemDto;
import org.kendar.utils.JsonMapper;

@SuppressWarnings("HttpUrlsUsage")
@TpmService
@HttpTypeFilter(
        blocking = true)
public class StorageHtmx implements FilteringClass {
    private final JsonMapper mapper;
    private final MultiTemplateEngine resolversFactory;
    private final DiService diService;
    private final StorageRepository repository;

    public StorageHtmx(JsonMapper mapper, MultiTemplateEngine resolversFactory,
                       DiService diService, StorageRepository repository) {
        this.mapper = mapper;
        this.resolversFactory = resolversFactory;
        this.diService = diService;
        this.repository = repository;
    }
    @Override
    public String getId() {
        return this.getClass().getName();
    }


    @HttpMethodFilter(
            pathAddress = "/storage",
            method = "GET", id = "GET /storage")
    public void storage(Request request, Response response) {
        var output = new StringOutput();
        resolversFactory.render("storage.jte",null,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/tree",
            method = "GET", id = "GET /storage/tree")
    public void storageMenu(Request request, Response response) {
        //var path = request.getQuery("path");
        //var splPath = path.split("/");
        var model = new FileTreeItemDto("",true);
        //if(splPath.length ==0) {
            model.getChildren().addAll(repository.listFiles().stream().
                    map(c->new FileTreeItemDto(c,false)).toList());
            model.getChildren().addAll(repository.listInstanceIds().stream().
                    map(instanceId->{
                        var f = new FileTreeItemDto(instanceId,true);
                        f.getChildren().addAll(
                                repository.listPluginIds(instanceId).stream().
                                        map(pluginId->{
                                            var pp = new FileTreeItemDto(pluginId,true);
                                            pp.getChildren().addAll(
                                                    repository.listPluginFiles(instanceId,pluginId).stream()
                                                            .map(pf->new FileTreeItemDto(pf.getIndex(),true))
                                                            .toList()
                                            );
                                            return pp;
                                        }).toList()
                        );
                        return f;
                    }).toList());
        var output = new StringOutput();
        resolversFactory.render("storage/tree.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }
}
