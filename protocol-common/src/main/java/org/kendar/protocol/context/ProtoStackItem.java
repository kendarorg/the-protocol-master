package org.kendar.protocol.context;

import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.special.SpecialProtoState;

import java.util.Stack;

public class ProtoStackItem {
    protected ProtoState state;
    protected Stack<ProtoState> executable = new Stack<>();

    public ProtoStackItem(ProtoState state) {

        this.state = state;
        reset();
    }

    public ProtoState getState() {
        return state;
    }

    public boolean canRun() {
        return !executable.empty() && (state instanceof SpecialProtoState);
    }

    public boolean isEmpty() {
        return executable.empty();
    }

    public ProtoState getNextExecutable() {
        return executable.pop();
    }

    public ProtoState peekNextExecutable() {
        return executable.peek();
    }

    public void reset() {
        if (state instanceof SpecialProtoState) {
            this.executable = new Stack<>();

            var children = ((SpecialProtoState) state).getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                var child = children.get(i);
                this.executable.add(child);
            }
        }
    }

    @Override
    public String toString() {
        if (this.executable.empty()) {
            return state.toString() + " (-)";
        }
        return state.toString() + " (" + this.executable.peek() + ")";
    }

    public int getSize() {
        return executable.size();
    }
}
