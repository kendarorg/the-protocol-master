package org.kendar.ui;

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
        resolversFactory.render("storage.jte", null, response);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/tree",
            method = "GET", id = "GET /storage/tree")
    public void storageDirs(Request request, Response response) {
        var path = request.getQuery("parent");
        var split = path.split("/");
        var close = request.getQuery("close") != null && request.getQuery("close").
                equalsIgnoreCase("true");

        var model = new FileTreeItemDto(path, true);
        model.setOpen(!close);
        if (!close) {
            model.getChildren().addAll(repository.listDirs(path).stream().
                    map(instanceId -> new FileTreeItemDto(path, instanceId, true)).toList());
        }

        resolversFactory.render("storage/tree.jte", model, response);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/files",
            method = "GET", id = "GET /storage/files")
    public void storageFiles(Request request, Response response) {
        var path = request.getQuery("parent");
        var split = path.split("/");
        var close = request.getQuery("close") != null && request.getQuery("close").
                equalsIgnoreCase("true");

        var model = new FileTreeItemDto(path, true);
        model.setOpen(!close);
        if (!close) {
            model.getChildren().addAll(repository.listFiles(path).stream().
                    map(instanceId -> new FileTreeItemDto(path, instanceId, false)).toList());
        }

        resolversFactory.render("storage/files.jte", model, response);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/file",
            method = "GET", id = "GET /storage/file")
    public void storageFile(Request request, Response response) {
        var path = request.getQuery("parent");
        var model = new FileItemDto();

        var data = repository.readFile(path);
        model.setContent(data);
        model.setPath(path);
        resolversFactory.render("storage/file.jte", model, response);
    }


    @HttpMethodFilter(
            pathAddress = "/storage/file",
            method = "POST", id = "POST /storage/file")
    public void storageFileCreateUpdate(Request request, Response response) {
        var path = request.getQuery("parent");
        var sentContent = request.getRequestText().asText();
        repository.writeFile(sentContent, path);
        response.addHeader("Content-type", "text/html");
        response.setStatusCode(200);
    }

    @HttpMethodFilter(
            pathAddress = "/storage/file",
            method = "DELETE", id = "DELETE /storage/file")
    public void storageFileDelete(Request request, Response response) {
        var path = request.getQuery("parent");
        repository.deleteFile(path);
        response.addHeader("Content-type", "text/html");
        response.setStatusCode(200);
    }
}
