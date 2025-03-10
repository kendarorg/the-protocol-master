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
import org.kendar.ui.dto.FileItemDto;
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
    public void storageDirs(Request request, Response response) {
        var path = request.getQuery("parent");
        var split = path.split("/");
        var close = request.getQuery("close")!=null && request.getQuery("close").
                equalsIgnoreCase("true");

        var model = new FileTreeItemDto(path,true);
        model.setOpen(!close);
        if(!close) {
            model.getChildren().addAll(repository.listDirs(path).stream().
                    map(instanceId -> new FileTreeItemDto(path, instanceId, true)).toList());
        }

        var output = new StringOutput();
        resolversFactory.render("storage/tree.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/files",
            method = "GET", id = "GET /storage/files")
    public void storageFiles(Request request, Response response) {
        var path = request.getQuery("parent");
        var split = path.split("/");
        var close = request.getQuery("close")!=null && request.getQuery("close").
                equalsIgnoreCase("true");

        var model = new FileTreeItemDto(path,true);
        model.setOpen(!close);
        if(!close) {
            model.getChildren().addAll(repository.listFiles(path).stream().
                    map(instanceId -> new FileTreeItemDto(path, instanceId, false)).toList());
            /*if (path.isEmpty()) {
                if (repository.listFiles().stream().anyMatch(f -> f.equalsIgnoreCase("settings"))) {
                    model.getChildren().add(new FileTreeItemDto(path, "settings", false));
                }
            } else if (split.length == 1) {
                if (split[0].equals("recordings")) {
                    model.getChildren().addAll(repository.listFiles().stream().
                            filter(f -> !f.equalsIgnoreCase("settings")).
                            map(instanceId -> new FileTreeItemDto(path, instanceId, false)).toList());
                }
            } else if (split.length == 2) {
                model.getChildren().addAll(repository.listPluginFiles(split[0], split[1]).stream().
                        map(f -> new FileTreeItemDto(path, f.getIndex(), false)).toList());
            } else {
                throw new RuntimeException("Invalid path ");
            }*/
        }

        var output = new StringOutput();
        resolversFactory.render("storage/files.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/file",
            method = "GET", id = "GET /storage/file")
    public void storageFile(Request request, Response response) {
        var path = request.getQuery("parent");
        var split = path.split("/");
        var model = new FileItemDto();

            var data = repository.readFile(path);
            model.setContent(data);
            model.setPath(path);
//
//            if (path.isEmpty()) {
//                var settings = repository.getSettings();
//                if (settings != null) {
//                    model.setContent(settings);
//                    model.setPath("");
//                    model.setName("settings");
//                }
//            } else if (split.length == 2) {
//                if (split[0].equals("recordings")) {
//                    var splittedname= split[1].split("\\.");
//                    if(splittedname[0].equalsIgnoreCase("index")) {
//                        var fc = repository.getIndexes(splittedname[1]);
//                        model.setContent(mapper.serializePretty(fc));
//                    }else {
//                        var fc = repository.readFromScenarioById(splittedname[1], Integer.parseInt(splittedname[0]));
//                        model.setContent(mapper.serializePretty(fc));
//                    }
//                    model.setPath(split[0]);
//                    model.setName(split[1]);
//                }
//            } else if (split.length == 3) {
//                var fc = repository.readPluginFile(new StorageFileIndex(split[0],split[1],split[2]));
//                model.setContent(mapper.serializePretty(fc));
//                model.setPath(split[0]);
//                model.setName(split[1]);
//            } else {
//                throw new RuntimeException("Invalid path ");
//            }

        var output = new StringOutput();
        resolversFactory.render("storage/file.jte",model,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }
}
