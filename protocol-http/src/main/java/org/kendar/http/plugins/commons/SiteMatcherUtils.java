package org.kendar.http.plugins.commons;

import org.kendar.apis.base.Request;

import java.util.ArrayList;
import java.util.List;

public class SiteMatcherUtils {
    public static List<MatchingRecRep> setupSites(List<String> target) {
        if (target == null || target.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(target.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(MatchingRecRep::new).toList());
    }

    public static boolean matchSite(Request site, List<MatchingRecRep> sites) {
        if (sites == null || sites.isEmpty()) {
            return true;
        }

        var matchFound = false;
        for (var pat : sites) {
            if (pat.match(site.getHost() + site.getPath())) {// || pat.toString().equalsIgnoreCase(request.getHost())) {
                matchFound = true;
                break;
            }
        }
        return matchFound;
    }
}
