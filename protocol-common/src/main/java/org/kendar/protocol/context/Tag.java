package org.kendar.protocol.context;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tag (key-value pair)
 */
public class Tag {
    private final String key;
    private final String value;

    public Tag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Build from a list of key-value pairs
     *
     * @param kvp
     * @return
     */
    public static List<Tag> of(String... kvp) {
        var result = new ArrayList<Tag>();
        for (var i = 0; i < (kvp.length / 2); i += 2) {
            result.add(new Tag(kvp[i], kvp[i + 1]));
        }
        return result;
    }

    /**
     * Build a list of keys
     *
     * @param k
     * @return
     */
    public static List<String> ofKeys(String... k) {
        return List.of(k);
    }

    /**
     * Convert a list to standard tags format
     *
     * @param tags
     * @return
     */
    public static String toString(List<Tag> tags) {
        if (tags == null) return "";
        return tags.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + ":" + value;
    }
}
