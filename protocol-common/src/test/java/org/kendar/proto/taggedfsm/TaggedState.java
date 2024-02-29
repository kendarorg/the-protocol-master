package org.kendar.proto.taggedfsm;

import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.HandlerDefinition;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public class TaggedState extends ProtoState implements HandlerDefinition<TaggedEvent> {
    public final String handledData;

    public TaggedState(String handledData, Class<?>... events) {
        super(events);
        this.handledData = handledData;
    }


    @Override
    public boolean canRun(TaggedEvent event) {
        return event.data.equalsIgnoreCase(handledData);
    }

    @Override
    public Iterator<ProtoStep> execute(TaggedEvent event) {
        var toOut = "EXECUTING " + event.data;
        if (event.getTagKeyValues() != null && !event.getTagKeyValues().isEmpty()) {
            toOut += " (" + event.getTagKeyValues() + ")";
        }
        System.out.println(toOut);
        return iteratorOfEmpty();
    }

    @Override
    public String toString() {
        return "State " + handledData;
    }
}
