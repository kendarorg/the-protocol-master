package org.kendar.apis;


import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.filters.StaticWebFilter;
import org.kendar.di.annotations.TpmService;
import org.kendar.utils.FileResourcesUtils;

@TpmService
@HttpTypeFilter()
public class MainWebSite extends StaticWebFilter {
    public MainWebSite(FileResourcesUtils fileResourcesUtils) {

        super(fileResourcesUtils);
    }

    @Override
    public String getId() {
        return "org.kendar.docker.MainWebSite";
    }

    @Override
    protected String getPath() {
        return "*web";
    }
}
