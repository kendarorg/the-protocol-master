package org.kendar.http.plugins;

import java.util.regex.Pattern;

public class MatchingRecRep {
    private Pattern pattern;
    private String[] exact;

    public MatchingRecRep(String item) {
        if (item.startsWith("@")) {
            this.pattern = Pattern.compile(item.substring(1));
        } else {
            this.exact = item.split("/");
        }
    }

    public boolean match(String rec) {
        if (pattern != null) {
            return pattern.matcher(rec).matches();
        }
        var expect = rec.split("/");
        if (expect.length < exact.length) {
            return false;
        }
        for (int i = 0; i < exact.length; i++) {
            var ex = exact[i];
            var px = expect[i];
            if (ex.equalsIgnoreCase("*")) continue;
            if (ex.equalsIgnoreCase(px)) continue;
            return false;
        }
        return true;
    }
}
