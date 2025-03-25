package org.kendar.tests.utils;

import java.util.HashMap;
import java.util.Locale;

public class Utils {
    private static final HashMap<String, Object> cache = new HashMap<>();

    public static void killApacheLogger() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "error");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "error");
        System.setProperty("log4j.logger.org.apache.http", "error");
        System.setProperty("log4j.logger.org.apache.http.wire", "error");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "error");
    }

    public static <T> T getCache(String key) {
        return (T) cache.get(key.toLowerCase(Locale.ROOT));
    }

    public static HashMap<String, Object> getCache(){
        return cache;
    }

    public static void setCache(String key, Object value) {
        if (value == null) {
            cache.remove(key.toLowerCase(Locale.ROOT));
        } else {
            cache.put(key.toLowerCase(Locale.ROOT), value);
        }
    }
}
