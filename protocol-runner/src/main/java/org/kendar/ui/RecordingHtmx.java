package org.kendar.ui;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmService;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService
@HttpTypeFilter(
        blocking = true)
public class RecordingHtmx implements FilteringClass {
    private final JsonMapper mapper;
    private final MultiTemplateEngine resolversFactory;
    private final DiService diService;
    private final StorageRepository repository;

    public RecordingHtmx(JsonMapper mapper, MultiTemplateEngine resolversFactory,
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
            pathAddress = "/recording",
            method = "GET", id = "GET /scenario")
    public void storage(Request request, Response response) {
        resolversFactory.render("recording.jte",null,response);
    }
}
