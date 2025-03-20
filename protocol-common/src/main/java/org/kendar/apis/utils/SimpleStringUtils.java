package org.kendar.apis.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleStringUtils {
    public static String[] splitByString(String separator, String toSplit) {
        var result = new String[2];
        var pos = toSplit.toLowerCase(Locale.ROOT).indexOf(separator.toLowerCase(Locale.ROOT));
        if (pos < 0) return result;
        result[0] = toSplit.substring(0, pos);
        result[1] = toSplit.substring(pos + separator.length());
        return result;
    }

    public static String shorten(String input, int length) {
        if (input.length() <= length) return input;
        return input.substring(0, length);
    }

    public static String convertTime(long time) {
        Date date = new Date(time);
        var format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return format.format(date);
    }
}
