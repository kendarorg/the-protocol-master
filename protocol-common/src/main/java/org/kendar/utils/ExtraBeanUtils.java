package org.kendar.utils;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExtraBeanUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ExtraBeanUtils.class);
    private static ConcurrentHashMap<String, Map<String, TypeOfProp>> cache = new ConcurrentHashMap<>();

    private static Map<String, TypeOfProp> describe(Object orig) {
        var pu = BeanUtilsBean.getInstance().getPropertyUtils();
        var origDescriptors = pu.getPropertyDescriptors(orig);
        var result = new HashMap<String, TypeOfProp>();
        for (PropertyDescriptor origDescriptor : origDescriptors) {
            final String name = origDescriptor.getName();
            if ("class".equals(name)) {
                continue; // No point in trying to set an object's class
            }
            if (pu.isReadable(orig, name)) {
                if (pu.isWriteable(orig, name)) {
                    result.put(name, TypeOfProp.BOTH);
                } else {
                    result.put(name, TypeOfProp.READ);
                }
            } else if (pu.isWriteable(orig, name)) {

                result.put(name, TypeOfProp.WRITE);
            }
        }
        return result;
    }

    public static void copyProperties(final Object dest, final Object orig, String... avoid) throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        var mapToAvoid = new HashSet<String>();
        for (var item : avoid) {
            mapToAvoid.add(item.toLowerCase(Locale.ROOT));
        }
        var origProperties = cache.computeIfAbsent(orig.getClass().getName(), k -> {
            return describe(orig);
        });
        var destProperties = cache.computeIfAbsent(dest.getClass().getName(), k -> {
            return describe(dest);
        });
        var pu = BeanUtilsBean.getInstance().getPropertyUtils();
        var bbu = BeanUtilsBean.getInstance();
        for (var origEntry : origProperties.entrySet()) {
            if (mapToAvoid.contains(origEntry.getKey().toLowerCase())) continue;
            var destEntry = destProperties.get(origEntry.getKey());
            if (destEntry == null) continue;
            if (
                    (origEntry.getValue() == TypeOfProp.READ || origEntry.getValue() == TypeOfProp.BOTH) &&
                            (destEntry == TypeOfProp.WRITE || destEntry == TypeOfProp.BOTH)) {
                var origValue = pu.getSimpleProperty(orig, origEntry.getKey());
                bbu.copyProperty(dest, origEntry.getKey(), origValue);
            }

        }

    }
}
