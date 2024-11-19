package org.kendar.utils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("StringConcatenationInLoop")
public class ExtraStringReplacer {
    public static List<String> parse(String input) {
        var result = new ArrayList<String>();
        char[] charArray = input.toCharArray();
        String sb = "";
        var previousIsParameter = false;
        for (int i = 0; i < charArray.length; i++) {
            var ch = charArray[i];
            if ((ch == '$' || ch == '@') && (i + 1) < charArray.length) {
                if (charArray[i + 1] == '{') {
                    var start = i + 2;
                    String paramName = ch + "{";
                    for (; start < charArray.length; start++) {
                        var ch2 = charArray[start];
                        if ((ch2 >= 65 && ch2 <= 90) || (ch2 >= 97 && ch2 <= 122)) {
                            paramName += ch2;
                        } else if (ch2 == '}') {
                            if (previousIsParameter) {
                                throw new RuntimeException();
                            }
                            result.add(sb);
                            sb = "";
                            paramName += ch2;
                            result.add(paramName);
                            previousIsParameter = true;
                            i = start;
                            break;
                        } else {
                            paramName = null;
                            break;
                        }
                    }
                    if (paramName == null) {

                        previousIsParameter = false;
                        sb += ch;
                    }
                } else {

                    previousIsParameter = false;
                    sb += ch;
                }
            } else {

                previousIsParameter = false;
                sb += ch;
            }
        }
        if (!sb.isEmpty()) {
            result.add(sb);
        }
        return result;
    }

    public static List<String> match(List<String> possiblePath, String query) {
        try {
            var result = new ArrayList<String>();
            var start = 0;
            for (int i = 0; i < possiblePath.size(); i++) {
                String path = possiblePath.get(i);
                if (isTemplate(path)) {
                    if (possiblePath.size() > i + 1) {
                        var next = possiblePath.get(i + 1);
                        var endOfParam = query.indexOf(next, start);
                        result.add(query.substring(start, endOfParam));
                        start = endOfParam;
                    } else {
                        result.add(query.substring(start));
                    }
                } else {
                    var startOfString = query.indexOf(path, start);
                    result.add(query.substring(startOfString, startOfString + path.length()));
                    start = startOfString + path.length();
                }
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static boolean isTemplate(String path) {
        return (path.startsWith("@{") || path.startsWith("${")) && path.endsWith("}");
    }
}
