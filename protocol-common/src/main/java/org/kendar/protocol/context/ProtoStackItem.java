package org.kendar.protocol.context;

import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.TaggedObject;
import org.kendar.protocol.states.special.SpecialProtoState;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Represent the instance of a state during an execution
 */
public class ProtoStackItem implements TaggedObject {

    /**
     * The state definition
     */
    protected final ProtoState state;
    /**
     * STore the id
     */
    private final String id;
    /**
     * The stack of possible sub states to execute
     */
    protected Stack<ProtoState> executable = new Stack<>();

    /**
     * Tags
     */
    private List<Tag> tags;

    /**
     * Given an event and its tags, and a state matching create a state instance
     *
     * @param state
     * @param event
     */
    public ProtoStackItem(ProtoState state, BaseEvent event,String id) {
        this.id = id;
        this.tags = new ArrayList<>();
        this.state = state;
        if (event != null) {
            this.tags = ((TaggedObject) event).getTag();
        }
        reset();
    }

    /**
     * If state has value
     *
     * @return
     */
    public boolean hasState() {
        return state != null;
    }

    /**
     * Retrieve the current state
     *
     * @return
     */
    public ProtoState getState() {
        return state;
    }

    /**
     * True if can do something and is a special state
     *
     * @return
     */
    public boolean canRun() {
        return !executable.empty() && (state instanceof SpecialProtoState);
    }

    /**
     * Has no state to execute
     *
     * @return
     */
    public boolean isEmpty() {
        return executable.empty();
    }

    /**
     * Pop the new item to execute
     *
     * @return
     */
    public ProtoState getNextExecutable() {
        return executable.pop();
    }

    /**
     * RE-initialize the executable (typically for first run or loops)
     */
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
        return state.toString() + " (" + this.executable.peek().getClass().getSimpleName() + ")";
    }

    /**
     * Get size of things
     *
     * @return
     */
    public int getSize() {
        return executable.size();
    }

    /**
     * Get the tags list
     *
     * @return
     */
    @Override
    public List<Tag> getTag() {
        return tags;
    }

    /**
     * Get the id
     *
     * @return
     */
    public String getId() {
        return id;
    }
}
