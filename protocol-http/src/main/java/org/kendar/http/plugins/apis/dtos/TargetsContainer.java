package org.kendar.http.plugins.apis.dtos;

import java.util.ArrayList;
import java.util.List;

public class TargetsContainer {
    private List<String> target = new ArrayList<>();
    private String api;

    public TargetsContainer(List<String> target, String api) {
        this.target = target;
        this.api = api;
    }

    public List<String> getTarget() {
        return target;
    }

    public void setTarget(List<String> target) {
        this.target = target;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }
}
