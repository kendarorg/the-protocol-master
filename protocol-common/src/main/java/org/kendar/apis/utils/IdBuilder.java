package org.kendar.apis.utils;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;

public class IdBuilder {

    public static String buildId(HttpTypeFilter type, HttpMethodFilter method, Object clazz, String id) {
        if (id != null && !id.isEmpty()) {
            return id;
        }
        String result = "";
        if (clazz != null) {
            if (clazz.equals(String.class)) {
                result += clazz + ":";
            } else {
                result += clazz.getClass().getSimpleName() + ":";
            }
        }
        result += method.method() + ":";
        if (type.hostPattern() != null && !type.hostPattern().isEmpty()) {
            result += type.hostPattern() + "/";
        } else {
            result += type.hostAddress() + "/";
        }
        if (method.pathPattern() != null && !method.pathPattern().isEmpty()) {
            result += method.pathPattern();
        } else {
            result += method.pathAddress();
        }
        result += ":" + type.priority();
        return result;
    }
}
