package org.kendar.protocol.context;


import org.kendar.exceptions.AskMoreDataException;
import org.kendar.exceptions.FailedStateException;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.FailedState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.Stop;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.protocol.states.special.SpecialProtoState;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.kendar.protocol.descriptor.ProtoDescriptor.getNow;

/**
 * Instance of a protocol definition
 */
public abstract class ProtoContext {

    /**
     * The executor to run asynchronously the system
     */
    protected static final ExecutorService executorService =
            new ThreadPoolExecutor(1, 100, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());
    /**
     * Stores the variable relatives to the current instance execution
     */
    protected final Map<String, Object> values = new HashMap<>();
    protected final int contextId;
    /**
     * The descriptor of the protocol
     */
    protected final ProtoDescriptor descriptor;
    /**
     * Flag to stop the execution
     */
    protected final AtomicBoolean run = new AtomicBoolean(true);
    private final Logger log = LoggerFactory.getLogger(ProtoContext.class);
    private final AtomicLong lastAccess = new AtomicLong(getNow());
    /**
     * Contains the -DECLARATION- of the protocol
     */
    private final Map<String, ProtoState> root;
    /**
     * Exclusively lock the send operation
     */
    private final Object sendLock = new Object();
    private final ConcurrentLinkedQueue<ProtoState> states = new ConcurrentLinkedQueue<>();
    /**
     * Execution stack, this stores the current state
     */
    protected Map<String, Stack<ProtoStackItem>> executionStack;
    /**
     * Contains the list of executed states to avoid recursion
     */
    private HashSet<String> recursionBlocker;
    /**
     * The last state used
     */
    private ProtoState currentState;
    private boolean useCallDurationTimes;

    public ProtoContext(ProtoDescriptor descriptor, int contextId) {
        this.contextId = contextId;
        this.descriptor = descriptor;
        this.root = descriptor.getTaggedStates();
        lastAccess.set(getNow());
        descriptor.getContextsCache().put(this.contextId, this);
    }

    /**
     * Check if a state is a "standard" state
     *
     * @param state
     * @return
     */
    private static boolean isNormalState(ProtoState state) {
        return !(state instanceof SpecialProtoState);
    }

    /**
     * Check if the possibleChild is a child of parentTag
     *
     * @param parentTag     channel:1
     * @param possibleChild channel:1,transaction:2
     * @return
     */
    private static boolean isSubKey(String parentTag, String possibleChild) {
        if (!parentTag.isEmpty()) {
            return possibleChild.startsWith(parentTag + ",");
        } else {
            return true;
        }
    }

    /**
     * Retrieve the executable state
     *
     * @param candidate
     * @param event
     * @return
     */
    private static ProtoState retrieveExecutableState(ProtoState candidate, ProtocolEvent event) {
        ProtoState result;
        if (candidate.canRunEvent(event)) {
            result = candidate;
        } else {
            result = new FailedState("Unable to run event", candidate, event);
        }
        return result;
    }

    public long getLastAccess() {
        return lastAccess.get();
    }

    public ProtoDescriptor getDescriptor() {
        return descriptor;
    }

    public void updateLastAccess() {
        lastAccess.set(getNow());
    }

    public abstract void disconnect(Object connection);

    /**
     * The instance id
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Store a value in the current instance
     *
     * @param key
     * @param value
     */
    public void setValue(String key, Object value) {
        key = key.toUpperCase();
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    /**
     * Retrieve a value from the current instance
     *
     * @param key
     * @return
     */
    public Object getValue(String key) {
        key = key.toUpperCase();
        return values.get(key);
    }

    /**
     * Retrieve a value from the current instance with default
     *
     * @param key
     * @param defaultResult
     * @param <E>
     * @return
     */
    public <E> E getValue(String key, E defaultResult) {
        key = key.toUpperCase();
        if (!values.containsKey(key)) return defaultResult;
        return (E) values.get(key);
    }

    /**
     * Send a message and execute it
     *
     * @param event
     * @return
     */
    public Future<Boolean> send(ProtocolEvent event) {
        lastAccess.set(getNow());
        return executorService.submit(() -> {
            try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", contextId + "")) {
                synchronized (sendLock) {
                    return reactToEvent(event);
                }
            }
        });
    }

    /**
     * Send a message synchronously
     *
     * @param event
     * @return
     */
    public boolean sendSync(ProtocolEvent event) {

        try (final MDC.MDCCloseable mdc = MDC.putCloseable("connection", contextId + "")) {
            return reactToEvent(event);
        }
    }

    /**
     * Find the correct handler for the event and execute it
     *
     * @param currentEvent
     * @return
     */
    public boolean reactToEvent(ProtocolEvent currentEvent) {
        try {
            lastAccess.set(getNow());
            log.trace("[CL>TP] RunFsmCycle");
            //Prepartion for the execution
            preExecute(currentEvent);
            //Find what should execute
            ProtoState foundedState = findThePossibleNextState(currentEvent);
            if (foundedState == null) {
                //May be next time
                return true;
            }

            if (currentEvent.getTagKeyValues() != null && !currentEvent.getTagKeyValues().isEmpty()) {
                log.debug("[CL>TP][RX]: Executing {} Tags: {}", foundedState.getClass().getSimpleName(), currentEvent.getTagKeyValues());
            } else {
                log.debug("[CL>TP][RX]: Executing {}", foundedState.getClass().getSimpleName());
            }
            currentState = foundedState;
            states.add(foundedState);
            while (states.size() > 5) {
                states.poll();
            }

            //Invoke the execution
            var stepsToInvoke = currentState.executeEvent(currentEvent);
            if (stepsToInvoke != null) {
                runSteps(stepsToInvoke, currentState, currentEvent);
            }
            //Post execution
            postExecute(currentEvent);
            return true;
        } catch (AskMoreDataException askMoreData) {
            //If more data is required just rethrow
            throw askMoreData;
        } catch (Exception ex) {
            //Real exception
            log.debug(ex.getMessage(), ex);
            //Execute the special handler
            handleExceptionInternal(ex);
            //Stop the execution
            return false;
        } finally {
            Sleeper.yield();
        }
    }

    /**
     * Seek the possible next state (interrupt or normal state)
     *
     * @param event
     * @return
     */
    private ProtoState findThePossibleNextState(ProtocolEvent event) {
        //Check if there are interrupts
        var possible = findPossibleInterrupt(event);
        ProtoState foundedState;
        //noinspection OptionalIsPresent
        if (possible.isEmpty()) {
            foundedState = findThePossibleNextStateOnStack(event, 0);
        } else {
            //If there is an interrupt run it!
            foundedState = possible.get();
        }
        if (foundedState == null) {
            return null;
        }
        if (foundedState instanceof FailedState) {
            throw new FailedStateException("[CL<TP][!!]: State: FailedState", foundedState, event);
        }
        return foundedState;
    }

    /**
     * Run the result of the invocation of the state
     *
     * @param stepsToRun
     * @param currentState
     * @param event
     */
    public void runSteps(Iterator<ProtoStep> stepsToRun, ProtoState currentState, ProtocolEvent event) {

        while (stepsToRun.hasNext()) {
            lastAccess.set(getNow());
            var steps = stepsToRun.next();
            if (steps == null) continue;
            if (steps.getClass() == Stop.class) {
                //Special state, termiante
                postStop(currentState);
                run.set(false);
                break;
            } else {
                //Run the step
                var stepResult = steps.run();
                if (stepResult == null) {
                    postWrite(null);
                    continue;
                }
                //Write somwhere the result
                log.debug("[CL<TP][TX]: Responding {} Tags: {}", stepResult.getClass().getSimpleName(), event.getTagKeyValues());
                write(stepResult);
                postWrite(stepResult);
            }
        }
    }

    protected void postWrite(ReturnMessage stepResult) {

    }

    /**
     * Override for termination
     *
     * @param executor
     */
    protected void postStop(ProtoState executor) {

    }

    /**
     * Override to send data outside
     *
     * @param returnMessage
     */
    public void write(ReturnMessage returnMessage) {

    }

    /**
     * Pre execute
     *
     * @param event
     */
    protected void preExecute(ProtocolEvent event) {
        var tagKey = event.getTagKeys();
        var tag = event.getTagKeyValues();

        //Initialize the execution stack
        if (executionStack == null) {
            executionStack = new HashMap<>();
        }
        //Prepare the tagged stack
        if (!executionStack.containsKey(tag)) {
            executionStack.put(tag, new Stack<>());
            executionStack.get(tag).add(new ProtoStackItem(root.get(tagKey), event, descriptor.getCounterString("STACK_ID")));
        }
        //Cleanup the recursion blocker
        recursionBlocker = new HashSet<>();
    }

    /**
     * Get the current state type
     *
     * @return
     */
    public Class<?> getCurrentState() {
        return currentState.getClass();
    }

    /**
     * If a state is not inhibited clean it up
     *
     * @param currentState
     * @param currentStateInstance
     */
    private void resetInstanceIfEmptyAndNotBlocked(ProtoState currentState, ProtoStackItem currentStateInstance) {
        if (!recursionBlocker.contains(currentState.getUuid() + Tag.toString(currentStateInstance.getTag())) && !currentStateInstance.canRun()) {
            currentStateInstance.reset();
        }
    }

    /**
     * If a state is falied
     *
     * @param result
     * @return
     */
    private boolean isFailed(ProtoState result) {
        return result instanceof FailedState;
    }

    /**
     * @param event
     * @param depth
     * @return
     */
    protected ProtoState findThePossibleNextStateOnStack(ProtocolEvent event, int depth) {
        if (depth > 10) {
            log.error("Max recursion hit searching for possible events");
            throw new RuntimeException("max recursion hit");
        }


        var eventTags = event.getTagKeyValues();
        ProtoState result = null;
        //While there is something to do
        do {
            //If nothing to do
            if (executionStack.get(eventTags).empty()) {
                return new FailedState("Machine interrupted");
            }
            //If executing a step while its parent had not been emptied/finished yet
            if (!parentAreClosed(eventTags)) {
                return new FailedState("Machine interrupted. Sub states not closed");
            }
            var currentStateInstance = executionStack.get(eventTags).peek();
            if (!currentStateInstance.hasState()) {
                //Something gone horribly wrong
                log.error("Missing state with tag {}", eventTags);
                return new FailedState("Missing State");
            }
            var currentState = currentStateInstance.getState();
            if (isNormalState(currentState) && currentState.canRunEvent(event)) {
                result = currentState;
            } else if (currentState instanceof ProtoStateWhile) {
                //Refill if it's empty
                resetInstanceIfEmptyAndNotBlocked(currentState, currentStateInstance);
                //Execute the loop
                result = executeLoop(currentStateInstance, event, depth);
            } else if (currentState instanceof ProtoStateSwitchCase) {
                //Refill if it's empty
                resetInstanceIfEmptyAndNotBlocked(currentState, currentStateInstance);
                //Execute the switch case
                result = executeSwitchCase(currentStateInstance, event, depth);
            } else if (currentState instanceof ProtoStateSequence) {
                //Refill if it's empty
                resetInstanceIfEmptyAndNotBlocked(currentState, currentStateInstance);
                //Execute the sequence
                result = executeSequence(currentStateInstance, event, depth);
            }
            //If it's a good thing stop it
        } while (isFailed(result));
        return result;
    }

    /**
     * Check if there are executions with longest tags (not closed)
     *
     * @param parentTags
     * @return
     */
    private boolean parentAreClosed(String parentTags) {
        //Iterate to all existing tags
        for (var executionTag : executionStack.keySet().toArray(new String[0])) {
            //Ignore itself
            if (executionTag.equalsIgnoreCase(parentTags)) continue;
            //Retrieve the current stack for the given tag
            var subExecutionStack = executionStack.get(executionTag);
            //If it's a sub of the parentTag
            if (isSubKey(parentTags, executionTag) && subExecutionStack != null && !subExecutionStack.empty()) {
                var stateInstance = subExecutionStack.peek();
                if (stateInstance == null) continue;
                //If there is nothing to do
                if (stateInstance.executable.empty()) continue;
                var state = stateInstance.getState();
                //If no state set
                if (state == null) continue;
                return false;
            }

        }
        return true;
    }

    /**
     * Run the special loop state (loops, can stop only before start or after end)
     *
     * @param currentInstance
     * @param event
     * @param depth           Recursion blocker
     * @return
     */
    private ProtoState executeLoop(ProtoStackItem currentInstance, ProtocolEvent event, int depth) {
        ProtoState result = null;
        var eventTags = event.getTagKeyValues();
        //While it can run (has something to execute)
        while (currentInstance.canRun()) {
            //Find who's next
            var candidate = currentInstance.getNextExecutable();
            result = getProtoState(candidate, event, eventTags, depth);
            //If it's an optional state just continue
            if (isFailed(result) && candidate.isOptional()) {
                continue;
            }
            //Always execute only the first good one
            break;
        }

        //If nothing more can be executed
        if (currentInstance.isEmpty() || isFailed(result)) {
            //Stop it for this cycle
            blockRecursionForCurrentStateInstance(currentInstance);
        }
        //If it's an error and there is something yet to do
        if (isFailed(result)) {
            popTheCurrentState(currentInstance, eventTags);
        }

        return result;
    }

    private ProtoState getProtoState(ProtoState candidate, ProtocolEvent event, String eventTags, int depth) {

        if (isNormalState(candidate)) {
            //If it's an executable return it
            return retrieveExecutableState(candidate, event);
        } else {
            if (shouldBlockRecursion(event, candidate)) {
                //Can't continue
                return new FailedState("Blocked recursion", candidate, event);
            } else {
                //Execute special states
                executionStack.get(eventTags).add(new ProtoStackItem(candidate, event, descriptor.getCounterString("STACK_ID")));
                return findThePossibleNextStateOnStack(event, depth + 1);
            }
        }
    }


    /**
     * If it's a possible recursion and should be blocked
     *
     * @param event
     * @param candidate
     * @return
     */
    private boolean shouldBlockRecursion(ProtocolEvent event, ProtoState candidate) {
        return recursionBlocker.contains(candidate.getUuid() + Tag.toString(event.getTag()));
    }


    private void popTheCurrentState(ProtoStackItem currentInstance, String eventTags) {
        if (executionStack.get(eventTags).empty()) return;
        var peek = executionStack.get(eventTags).peek();
        //Check if it's the last good one
        if (peek != null) {
            if (peek.getId().equalsIgnoreCase(currentInstance.getId()) && !executionStack.get(eventTags).empty()) {
                //And remove
                executionStack.get(eventTags).pop();
            }
        }
    }

    /**
     * Handle the switch case (only one can be executed)
     *
     * @param currentInstance
     * @param event
     * @param depth
     * @return
     */
    private ProtoState executeSwitchCase(ProtoStackItem currentInstance, ProtocolEvent event, int depth) {
        ProtoState result = null;
        var eventTags = event.getTagKeyValues();
        //The switch case can run ONLY if the available executors are all set
        if (currentInstance.getSize() != ((SpecialProtoState) currentInstance.getState()).getChildren().size()) {
            executionStack.get(eventTags).pop();
            return new FailedState("Missing data on switch case");
        }
        //While it can run (has something to execute)
        while (currentInstance.canRun()) {
            //Find who's next
            var candidate = currentInstance.getNextExecutable();
            result = getProtoState(candidate, event, eventTags, depth);
            //May be there is something more
            if (isFailed(result)) {
                continue;
            }
            //Always execute only the first good one
            break;
        }
        if (result == null) {
            result = new FailedState("Unable to run event", currentInstance.getState(), event);
        }
        blockRecursionForCurrentStateInstance(currentInstance);
        popTheCurrentState(currentInstance, eventTags);

        return result;
    }

    /**
     * Execute a sequence (all mandatory)
     *
     * @param currentInstance
     * @param event
     * @param depth
     * @return
     */
    private ProtoState executeSequence(ProtoStackItem currentInstance, ProtocolEvent event, int depth) {
        ProtoState result = null;
        var eventTags = event.getTagKeyValues();
        //While it can run (has something to execute)
        while (currentInstance.canRun()) {
            //Find who's next
            var candidate = currentInstance.getNextExecutable();
            result = getProtoState(candidate, event, eventTags, depth);
            //If it's an optional state just continue
            if (isFailed(result) && candidate.isOptional()) {
                continue;
            }
            //Always execute only the first good one
            break;
        }
        //If nothing more can be executed or there is an error
        if (currentInstance.isEmpty() || isFailed(result)) {
            blockRecursionForCurrentStateInstance(currentInstance);
            popTheCurrentState(currentInstance, eventTags);

        }

        return result;
    }

    /**
     * Consider this isntance as blocked
     *
     * @param currentInstance
     */
    private void blockRecursionForCurrentStateInstance(ProtoStackItem currentInstance) {
        recursionBlocker.add(currentInstance.getState().getUuid() + Tag.toString(currentInstance.getTag()));
    }

    /**
     * Post execute handler
     *
     * @param currentEvent
     */
    protected void postExecute(ProtocolEvent currentEvent) {

    }

    /**
     * Handle exception thrown during execution
     *
     * @param ex
     */
    public void handleExceptionInternal(Exception ex) {
        ProtocolEvent event = null;
        ProtoState state = null;
        if (ex instanceof FailedStateException) {
            event = ((FailedStateException) ex).getEvent();
            state = ((FailedStateException) ex).getState();
        }
        var exceptionResults = runException(ex, state, event);
        for (var exceptionResult : exceptionResults) {
            log.error("Exception handled: {}", exceptionResult.getClass().getSimpleName());
            write(exceptionResult);
        }
    }


    /**
     * Overridable exception handler
     *
     * @param ex
     * @param state
     * @param event
     * @return
     */
    protected List<ReturnMessage> runException(Exception ex, ProtoState state, ProtocolEvent event) {
        throw new RuntimeException(ex);
    }

    /**
     * Find possible interrupt given the event type
     *
     * @param currentEvent
     * @return
     */
    private Optional<ProtoState> findPossibleInterrupt(ProtocolEvent currentEvent) {
        return this.descriptor.getInterrupts().stream()
                .filter(s -> s.canHandle(currentEvent.getClass()))
                .filter(s -> s.canRunEvent(currentEvent))
                .findFirst();
    }

    public boolean isUseCallDurationTimes() {
        return useCallDurationTimes;
    }

    public void setUseCallDurationTimes(boolean useCallDurationTimes) {
        this.useCallDurationTimes = useCallDurationTimes;
    }
}
