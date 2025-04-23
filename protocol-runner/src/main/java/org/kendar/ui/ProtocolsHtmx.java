package org.kendar.ui;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;

@TpmService
@HttpTypeFilter(
        blocking = true)
public class ProtocolsHtmx implements FilteringClass {

    private final MultiTemplateEngine resolversFactory;

    public ProtocolsHtmx( MultiTemplateEngine resolversFactory) {

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
                sorted(java.util.Map.Entry.comparingByKey()).
                map(s -> data.getProtocolForKey(s.getKey())).toList();

        resolversFactory.render("protocols.jte", sortedProtocol, response);
    }


    @HttpMethodFilter(
            pathAddress = "/protocols/{protocolId}",
            method = "GET", id = "GET /protocols/{protocolId}")
    public void storeProtocolSetting(Request request, Response response) {
        var data = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
        var sortedProtocol = data.getProtocols().entrySet().stream().
                sorted(java.util.Map.Entry.comparingByKey()).
                map(s -> data.getProtocolForKey(s.getKey())).toList();

        resolversFactory.render("protocols.jte", sortedProtocol, response);
    }
}
