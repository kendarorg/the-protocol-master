package org.kendar.apis.matchers;

import org.kendar.apis.base.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PathSimpleMatcher {

    private List<String> pathSimpleMatchers = new ArrayList<>();

    public void setupPathSimpleMatchers(String pathAddress) {
        pathSimpleMatchers = new ArrayList<>();
        if (pathAddress != null && pathAddress.contains("{")) {
            var explTemplate = pathAddress.split("/");
            for (String s : explTemplate) {
                var partTemplate = s;
                if (partTemplate.startsWith("{")) {
                    partTemplate = partTemplate.substring(1);
                    partTemplate = "*" + partTemplate.substring(0, partTemplate.length() - 1);
                }
                pathSimpleMatchers.add(partTemplate);
            }
        }
    }

    public boolean notMatch(String real, String provided) {
        if (provided == null) return false;
        if (provided.equalsIgnoreCase("*")) return false;
        return !real.equalsIgnoreCase(provided);
    }

    public boolean matches(Request req) {
        var pathParams = new HashMap<String, String>();
        if (pathSimpleMatchers != null && !pathSimpleMatchers.isEmpty()) {
            var explPath = req.getPath().split("/");
            if (pathSimpleMatchers.size() != explPath.length) return false;
            for (var i = 0; i < pathSimpleMatchers.size(); i++) {
                var partTemplate = pathSimpleMatchers.get(i);
                var partPath = explPath[i];
                if (partTemplate.startsWith("*")) {
                    partTemplate = partTemplate.substring(1);
                    pathParams.put(partTemplate, partPath);
                } else if (!partTemplate.equalsIgnoreCase(partPath)) {
                    return false;
                }
            }
            for (var item : pathParams.entrySet()) {
                req.addPathParameter(item.getKey(), item.getValue());
            }
            return true;
        }
        return false;
    }
}
