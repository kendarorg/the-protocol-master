package org.kendar.protocol.states.special;

import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.states.ProtoState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SpecialProtoState extends ProtoState {
    protected List<ProtoState> children;

    public SpecialProtoState(ProtoState... states) {
        this.children = new ArrayList<>(Arrays.asList(states));
    }

    public List<ProtoState> getChildren() {
        return children;
    }

    public boolean canHandle(BaseEvent event) {
        var result = false;
        for (var child : children) {
            if (child.canHandle(event.getClass())) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean canRun(BaseEvent event) {
        return true;
    }
}
