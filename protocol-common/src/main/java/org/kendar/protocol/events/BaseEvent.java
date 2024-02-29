package org.kendar.protocol.events;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.states.TaggedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic event
 */
public class BaseEvent implements TaggedObject {

    /**
     * Context handling this event
     */
    private final ProtoContext context;

    /**
     * Previous state
     */
    private final Class<?> prevState;

    /**
     * Tags
     */
    private ArrayList<Tag> tags = new ArrayList<>();

    public BaseEvent(ProtoContext context, Class<?> prevState) {

        this.context = context;
        this.prevState = prevState;
    }

    public ProtoContext getContext() {
        return context;
    }

    public Class<?> getPrevState() {
        return prevState;
    }

    @Override
    public List<Tag> getTag() {
        return tags;
    }

    public void setTags(ArrayList<Tag> tags) {
        this.tags = tags;
    }

    /**
     * Retrieve all the key value pairs in a single string
     *
     * @return
     */
    public String getTagKeyValues() {
        return Tag.toString(tags);
    }

    /**
     * Retrieve the list of all the tag keys
     *
     * @return
     */
    public String getTagKeys() {
        if (tags == null) return "";
        return tags.stream().map(Tag::getKey).collect(Collectors.joining(","));
    }
}
