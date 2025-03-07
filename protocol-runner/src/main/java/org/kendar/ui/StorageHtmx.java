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
        var path = request.getQuery("parent");
        var split = path.split("/");

        var model = new FileTreeItemDto(path,true);
        if(path.isEmpty()){
            model.getChildren().add(new FileTreeItemDto(path,"recordings",true));
            model.getChildren().addAll(repository.listInstanceIds().stream().
                    map(instanceId->new FileTreeItemDto(path,instanceId,true)).toList());
            if(repository.listFiles().stream().anyMatch(f-> f.equalsIgnoreCase("settings"))){
                model.getChildren().add(new FileTreeItemDto(path,"settings",false));
            }
        }else if(split.length==1){
            if(split[0].equals("recordings")){
                model.getChildren().addAll(repository.listFiles().stream().
                        filter(f-> !f.equalsIgnoreCase("settings")).
                        map(instanceId->new FileTreeItemDto(path,instanceId,false)).toList());
            }else{
                model.getChildren().addAll(repository.listPluginIds(split[0]).stream().
                        map(instanceId->new FileTreeItemDto(path,instanceId,true)).toList());
            }
        }else if(split.length==2){
            model.getChildren().addAll(repository.listPluginFiles(split[0],split[1]).stream().
                    map(f->new FileTreeItemDto(path,f.getIndex(),false)).toList());
        }else{
            throw new RuntimeException("Invalid path ");
        }

        var output = new StringOutput();
        resolversFactory.render("storage/tree.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }
}
