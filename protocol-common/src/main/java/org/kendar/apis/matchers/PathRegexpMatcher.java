package org.kendar.apis.matchers;

import org.kendar.apis.base.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class PathRegexpMatcher {
    private static final Pattern namedGroupsPattern =
            Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z\\d]*)>");
    private List<String> pathMatchers;

    public void getNamedGroupCandidates(String pathPattern) {
        Set<String> matchedGroups = new TreeSet<>();
        var m = namedGroupsPattern.matcher(pathPattern);
        while (m.find()) {
            matchedGroups.add(m.group(1));
        }
        pathMatchers = new ArrayList<>(matchedGroups);
    }

    public boolean matches(Request req, Pattern pathPatternReal) {
        if (pathPatternReal != null) {
            var matcher = pathPatternReal.matcher(req.getPath());
            if (matcher.matches()) {
                for (String pathMatcher : pathMatchers) {
                    var group = matcher.group(pathMatcher);
                    if (group != null) {
                        req.addPathParameter(pathMatcher, group);
                    }
                }
                return true;
            }
        }
        return false;
    }
}
