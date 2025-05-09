package org.kendar.proto.taggedfsm;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.events.ProtocolEvent;

import java.util.ArrayList;

public class TaggedEvent extends ProtocolEvent {
    public final String data;

    public TaggedEvent(ProtoContext context, Class<?> prevState, String data, String... tagsKvp) {
        super(context, prevState);
        this.data = data;
        this.setTags(new ArrayList<Tag>());
        for (var i = 0; i < tagsKvp.length - 1; i += 2) {
            var key = tagsKvp[i];
            var value = tagsKvp[i + 1];
            this.getTag().add(new Tag(key, value));
        }
    }

    @Override
    public String toString() {
        return "TaggedEvent{" +
                "data='" + data + '\'' +
                '}';
    }


}
