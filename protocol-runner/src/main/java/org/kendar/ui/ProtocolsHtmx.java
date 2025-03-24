package org.kendar.ui;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmService;
import org.kendar.utils.JsonMapper;

@TpmService
@HttpTypeFilter(
        blocking = true)
public class ProtocolsHtmx implements FilteringClass {

    private final DiService diService;
    private final JsonMapper mapper;
    private final MultiTemplateEngine resolversFactory;

    public ProtocolsHtmx(DiService diService,JsonMapper mapper, MultiTemplateEngine resolversFactory) {
        this.diService = diService;
        this.mapper = mapper;
        this.resolversFactory = resolversFactory;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/protocols",
            method = "GET", id = "GET /protocols")
    public void retrieve(Request request, Response response) {
        var data = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
        var sortedProtocol = data.getProtocols().entrySet().stream().
                sorted(java.util.Comparator.comparing(java.util.Map.Entry::getKey)).
                map(s-> data.getProtocolForKey(s.getKey())).toList();

        resolversFactory.render("protocols.jte", sortedProtocol, response);
    }


    @HttpMethodFilter(
            pathAddress = "/protocols/{protocolId}",
            method = "GET", id = "GET /protocols/{protocolId}")
    public void storeProtocolSetting(Request request, Response response) {
        var data = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
        var sortedProtocol = data.getProtocols().entrySet().stream().
                sorted(java.util.Comparator.comparing(java.util.Map.Entry::getKey)).
                map(s-> data.getProtocolForKey(s.getKey())).toList();

        resolversFactory.render("protocols.jte", sortedProtocol, response);
    }
}
