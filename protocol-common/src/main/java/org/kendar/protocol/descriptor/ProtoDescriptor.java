package org.kendar.protocol.descriptor;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.special.SpecialProtoState;
import org.kendar.protocol.states.special.Tagged;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.TimerInstance;
import org.kendar.utils.TimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple protocol descriptor
 */
public abstract class ProtoDescriptor {
    /**
     * A map of [tag keys][states] one for each "tag" branch
     */
    protected final Map<String, ProtoState> taggedStates = new HashMap<>();
    private final AtomicInteger timeout = new AtomicInteger(30);
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final Logger log = LoggerFactory.getLogger(ProtoDescriptor.class);
    /**
     * Interrupt states, these are evaluated always, BEFORE the standard events
     */
    private final List<ProtoState> interrupts = new ArrayList<>();
    private ConcurrentHashMap<Integer, ProtoContext> contextsCache;
    private TimerInstance contextCleaner;

    public static long getNow() {
        return System.currentTimeMillis() / 1000;
    }

    public void setTimeout(int timeout) {
        this.timeout.set(timeout);
    }

    public ConcurrentHashMap<Integer, ProtoContext> getContextsCache() {
        return contextsCache;
    }

    public int getCounter(String id) {
        id = id.toUpperCase();
        return counters.computeIfAbsent(id, (key) ->
                new AtomicInteger(0)).incrementAndGet();
    }

    public String getCounterString(String id) {
        id = id.toUpperCase();
        return "" + counters.computeIfAbsent(id, (key) ->
                new AtomicInteger(0)).incrementAndGet();
    }

    public void cleanCounters() {
        counters.clear();
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
        start.setProtoDescriptor(this);
        initializeStatic(this);
        initializeTag("", start);
        initializeInternal(start, "");
    }

    private void contextsClean() {

        var fixedItemsList = new ArrayList<>(contextsCache.entrySet());
        for (var item : fixedItemsList) {
            var now = getNow();
            if (item.getValue().getLastAccess() < (now - timeout.get())) {
                var context = item.getValue();
                var contextConnection = context.getValue("CONNECTION");
                if (contextConnection == null) {
                    contextsCache.remove(item.getKey());
                }

                try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", context.getContextId() + "")) {
                    try {
                        context.disconnect(((ProxyConnection) contextConnection).getConnection());
                        log.debug("[DISCONNECT]");
                    } catch (Exception ex) {
                        log.trace("[DISCONNECT] Error", ex);
                    }
                }
                contextsCache.remove(item.getKey());
            } else {
                log.trace("[KEEPALIVE]");
            }
        }
    }

    protected void initializeStatic(ProtoDescriptor protoDescriptor) {
        contextsCache = new ConcurrentHashMap<>();
        if (contextCleaner != null) {
            contextCleaner.cancel();
        }
        var timerService = new TimerService();
        contextCleaner = timerService.schedule(this::contextsClean, 1000, 2000);
    }


    /**
     * Split the protocol in all the sub tags
     *
     * @param start
     * @param parentTags
     */
    protected void initializeInternal(ProtoState start, String parentTags) {
        start.setProtoDescriptor(this);
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
        start.setProtoDescriptor(this);
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
    public ProtoContext buildContext(int contextId) {
        return createContext(this, contextId);
    }

    public ProtoContext buildContext() {
        return buildContext(getCounter("CONTEXT_ID"));
    }

    /**
     * Override to create a custom context
     *
     * @param protoDescriptor
     * @return
     */
    protected abstract ProtoContext createContext(ProtoDescriptor protoDescriptor, int contextId);

    protected ProtoContext createContext(ProtoDescriptor protoContext) {
        return createContext(protoContext, protoContext.getCounter("CONTEXT_ID"));
    }

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
        state.setProtoDescriptor(this);
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

    public void terminate() {

    }

    public void start() {

    }
}
