package org.kendar.utils;

import java.util.regex.Pattern;

public class ReplacerItemInstance extends ReplacerItem {
    private final boolean trailing;
    private Pattern findPattern;

    public ReplacerItemInstance(ReplacerItem replacer, boolean trailing) {
        this.trailing = trailing;
        setToReplace(replacer.getToReplace().replaceAll("\r\n", "\n").trim());
        setRegex(replacer.isRegex());
        if (isRegex()) {
            var expr = replacer.getToFind().replaceAll("\r\n", "\n").trim();
            if (trailing) {
                expr += "(.*)";
            }
            findPattern = Pattern.compile(expr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        } else {
            setToFind(replacer.getToFind().replaceAll("\r\n", "\n").trim());
        }
    }

    public String run(String query) {
        if (isRegex()) {
            var matcher = findPattern.matcher(query);
            if (!matcher.matches()) return query;
            if (this.trailing) {
                var lastGroup = matcher.group(matcher.groupCount());
                return matcher.replaceAll(getToReplace()) + lastGroup;
            } else {
                return matcher.replaceAll(getToReplace());
            }
        } else {
            if (query.startsWith(getToFind())) {
                return getToReplace() + query.substring(getToFind().length());
            }
            return query;
        }
    }
}
