package org.kendar.protocol.descriptor;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.special.SpecialProtoState;
import org.kendar.protocol.states.special.Tagged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple protocol descriptor
 */
public abstract class ProtoDescriptor {

    private static final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ProtoDescriptor.class);
    /**
     * Interrupt states, these are evaluated always, BEFORE the standard events
     */
    private final List<ProtoState> interrupts = new ArrayList<>();
    /**
     * A map of [tag keys][states] one for each "tag" branch
     */
    protected final Map<String, ProtoState> taggedStates = new HashMap<>();

    public static int getCounter(String id) {
        id = id.toUpperCase();
        return counters.computeIfAbsent(id, (key) ->
                new AtomicInteger(0)).incrementAndGet();
    }

    public static String getCounterString(String id) {
        id = id.toUpperCase();
        return "" + counters.computeIfAbsent(id, (key) ->
                new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Initialize the protocol
     */
    public void initialize() {

        counters.clear();
        initializeProtocol();
    }

    /**
     * Build the protocol and spread the tags to the start map
     *
     * @param start
     */
    protected void initialize(ProtoState start) {
        buildProtocolDescription(start);
        initializeTag("", start);
        initializeInternal(start, "");
    }

    /**
     * Build a graphviz file for the protocol description
     *
     * @param start
     */
    private void buildProtocolDescription(ProtoState start) {
        log.warn("[SERVER] Not implemented ProtoDescriptor::buildProtocolDescription");
    }

    /**
     * Split the protocol in all the sub tags
     *
     * @param start
     * @param parentTags
     */
    protected void initializeInternal(ProtoState start, String parentTags) {
        if (start instanceof SpecialProtoState) {
            var spp = (SpecialProtoState) start;
            List<ProtoState> children = spp.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                var child = children.get(i);
                if (child instanceof Tagged) {
                    var tg = (Tagged) child;
                    var tagKeys = String.join(",", tg.getTags());
                    if (parentTags != null && !parentTags.isEmpty()) {
                        tagKeys = parentTags + "," + tagKeys;
                    }
                    initializeTag(tagKeys, tg.getChildren().get(0));
                    initializeInternal(tg.getChildren().get(0), tagKeys);
                    children.remove(i);
                } else {
                    initializeInternal(child, parentTags);
                }
            }
        }

    }

    /**
     * Add the specific state to the tag
     *
     * @param tag
     * @param start
     */
    protected void initializeTag(String tag, ProtoState start) {
        if (this.taggedStates.containsKey(tag)) throw new RuntimeException("Duplicate Tag");
        this.taggedStates.put(tag, start);
    }

    /**
     * Override and build here the protocol
     */
    protected abstract void initializeProtocol();


    /**
     * Create a context
     *
     * @return
     */
    public ProtoContext buildContext() {
        return createContext(this);
    }

    /**
     * Override to create a custom context
     *
     * @param protoDescriptor
     * @return
     */
    protected abstract ProtoContext createContext(ProtoDescriptor protoDescriptor);


    /**
     * Retrieve the list of interrupts
     *
     * @return
     */
    public List<ProtoState> getInterrupts() {
        return interrupts;
    }

    /**
     * Add a single interrupt
     *
     * @param state
     */
    protected void addInterruptState(ProtoState state) {
        if (!(state instanceof InterruptProtoState)) {
            throw new RuntimeException(state.getClass().getSimpleName() + " is not an InterruptProtoState");
        }
        interrupts.add(state);
    }


    /**
     * Retrieve all the tagged states
     *
     * @return
     */
    public Map<String, ProtoState> getTaggedStates() {
        return taggedStates;
    }
}
