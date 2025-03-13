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
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.JsonMapper;

@SuppressWarnings("HttpUrlsUsage")
@TpmService
@HttpTypeFilter(
        blocking = true)
public class MainHtmxPages implements FilteringClass {

    private final JsonMapper mapper;
    private final MultiTemplateEngine resolversFactory;
    private final DiService diService;

    public MainHtmxPages(JsonMapper mapper, MultiTemplateEngine resolversFactory, DiService diService) {
        this.mapper = mapper;
        this.resolversFactory = resolversFactory;
        this.diService = diService;
    }
    @Override
    public String getId() {
        return getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/",
            method = "GET", id = "GET /")
    public void root(Request request, Response response) {
        var output = new StringOutput();
        var settings = diService.getInstance(GlobalSettings.class);
        resolversFactory.render("index.jte",settings,output);
        response.addHeader("Content-type","text/html");
        response.setResponseText(new TextNode(output.toString()));
        response.setStatusCode(200);
    }

}
