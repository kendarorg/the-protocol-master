package org.kendar.protocol.states;

import org.kendar.protocol.context.Tag;

import java.util.List;

/**
 * Object containing tags (key-value pairs)
 */
public interface TaggedObject {
    /**
     * Retrieve the list of tags
     *
     * @return
     */
    List<Tag> getTag();
}
