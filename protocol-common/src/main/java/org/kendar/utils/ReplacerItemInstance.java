package org.kendar.utils;

import java.util.regex.Pattern;

public class ReplacerItemInstance extends ReplacerItem{
    public Pattern getFindPattern() {
        return findPattern;
    }

    private Pattern findPattern;

    public ReplacerItemInstance(ReplacerItem replacer) {
        setToReplace(replacer.getToReplace().replaceAll("\r\n", "\n").trim());
        setRegex(replacer.isRegex());
        if(isRegex()) {
            findPattern = Pattern.compile(replacer.getToFind().replaceAll("\r\n", "\n").trim(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }else{
            setToFind(replacer.getToFind().replaceAll("\r\n", "\n").trim());
        }
    }

    public String run(String query){
        if (isRegex()) {
            var matcher = findPattern.matcher(query);
            if(!matcher.matches())return query;
            return matcher.replaceFirst(getToReplace());
        } else {
            if (getToFind().equalsIgnoreCase(query)) {
                return getToReplace();
            }
            return query;
        }
    }
}
