package org.kendar.protocol.context;


import org.kendar.exceptions.AskMoreDataException;
import org.kendar.exceptions.FailedStateException;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BaseEvent;
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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProtoContext {
    private static final Logger log = LoggerFactory.getLogger(ProtoContext.class);
    protected final Map<String, Object> values = new HashMap<>();
    protected ProtoDescriptor descriptor;
    protected AtomicBoolean run = new AtomicBoolean(true);
    protected ConcurrentLinkedDeque<BaseEvent> inputQueue = new ConcurrentLinkedDeque<>();
    protected Stack<ProtoStackItem> executionStack;
    protected List<BaseEvent> orderedEvents = new ArrayList<>();
    private boolean transaction;
    private final ProtoState root;
    private HashSet<String> recursionBlocker;
    private ProtoState currentState;


    public ProtoContext(ProtoState root) {

        this.root = root;
    }

    public ProtoContext(ProtoDescriptor descriptor) {
        this.descriptor = descriptor;
        this.root = descriptor.getStart();
    }

    private static boolean isNormalState(ProtoState currentInstanceState) {
        return !(currentInstanceState instanceof SpecialProtoState);
    }

    public void setValue(String key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    public Object getValue(String key) {
        return values.get(key);
    }

    public void send(BaseEvent event) {
        inputQueue.add(event);
    }

    public void start() {
        preStart();
        while (run.get()) {
            if (!runFsmCycle()) {
                return;
            }
        }
        postEnd();
        log.trace("[SERVER] Context closed");
    }

    public void postEnd() {

    }

    public void preStart() {

    }

    private void retrieveEvents() {
        while (!inputQueue.isEmpty() && run.get()) {
            var event = inputQueue.poll();
            if (!customHandledEvent(event)) {
                orderedEvents.add(event);
            }
        }
    }

    protected boolean customHandledEvent(BaseEvent event) {
        return false;
    }

    protected BaseEvent findCurrentEvent() {
        try {
            if (!orderedEvents.isEmpty()) {
                return orderedEvents.remove(0);
            }
            return null;
        }finally {
            Sleeper.yield(10);
        }
    }

    public boolean runFsmCycle() {
        try {
            retrieveEvents();
            var currentEvent = findCurrentEvent();
            if (currentEvent == null) {
                return true;
            }
            initialize();
            var possible = getPossibleInterrupt(currentEvent);
            ProtoState foundedState = null;
            if (possible.isPresent()) {
                foundedState = possible.get();
            } else {
                foundedState = runInternal(currentEvent);
            }
            if (foundedState instanceof FailedState) {
                throw new FailedStateException("[SERVER] State: FailedState", foundedState, currentEvent);
            }
            if (foundedState == null) {
                return true;
            }

            log.debug("[SERVER][RX]: " + foundedState.getClass().getSimpleName());
            currentState = foundedState;
            var stepsToInvoke = currentState.executeEvent(currentEvent);
            postExecute();
            if (stepsToInvoke != null) {
                runSteps(stepsToInvoke, currentState);
            }
            return true;
        }catch (AskMoreDataException ex) {
            return true;
        }catch (Exception ex) {
            runException(ex);
            return false;
        }finally {
            Sleeper.yield(10);
        }
    }

    public void runSteps(Iterator<ProtoStep> stepsToInvoke, ProtoState executor) {
        while (stepsToInvoke.hasNext()) {
            var steps = stepsToInvoke.next();
            if (steps == null) continue;
            if (steps.getClass() == Stop.class) {
                postStop(executor);
                run.set(false);
                break;
            } else {
                var stepResult = steps.run();
                if (stepResult == null) continue;
                log.debug("[SERVER][TX]: " + stepResult.getClass().getSimpleName());
                write(stepResult);
            }
        }
    }

    protected void postStop(ProtoState executor) {

    }

    public void write(ReturnMessage returnMessage) {

    }

    private void initialize() {
        if (executionStack == null) {
            executionStack = new Stack<>();
            executionStack.add(new ProtoStackItem(root));
        }
        recursionBlocker = new HashSet<>();
    }

    public ProtoState run(BaseEvent event) {
        initialize();
        currentState = runInternal(event);
        if (currentState instanceof FailedState) {
            return currentState;
        }
        currentState.executeEvent(event);
        return currentState;
    }

    public Class<?> getCurrentState() {
        return currentState.getClass();
    }

    private void resetInstanceIfEmptyAndNotBlocked(ProtoState currentInstanceState, ProtoStackItem currentInstance) {
        if (!recursionBlocker.contains(currentInstanceState.getUuid()) && !currentInstance.canRun()) {
            currentInstance.reset();
        }
    }


    private ProtoState runLoopState(ProtoStackItem currentInstance, BaseEvent event) {
        ProtoState result = null;
        while (currentInstance.canRun()) {
            var candidate = currentInstance.getNextExecutable();
            if (isNormalState(candidate)) {
                if (candidate.canRunEvent(event)) {
                    result = candidate;
                } else {
                    result = new FailedState("Unable to run event", candidate, event);
                }
            } else {
                if (recursionBlocker.contains(candidate.getUuid())) {
                    result = new FailedState("Blocked recursion", candidate, event);
                } else {
                    executionStack.add(new ProtoStackItem(candidate));
                    result = runInternal(event);
                }
            }
            if (isFailed(result) && candidate.isOptional()) {
                continue;
            }
            break;
        }
        if (currentInstance.isEmpty() || isFailed(result)) {
            recursionBlocker.add(currentInstance.getState().getUuid());
        }
        if (isFailed(result) && !executionStack.empty()) {
            var peek = executionStack.peek();
            if (peek.getState().getUuid().equalsIgnoreCase(currentInstance.getState().getUuid())) {
                executionStack.pop();
            }
        }

        return result;
    }

    private boolean isFailed(ProtoState result) {
        return result instanceof FailedState;
    }

    protected ProtoState runInternal(BaseEvent event) {

        ProtoState result = null;
        while (true) {
            if (executionStack.empty()) {
                return new FailedState("Machine interrupted");
            }
            var currentInstance = executionStack.peek();
            var currentInstanceState = currentInstance.getState();
            if (isNormalState(currentInstanceState) && currentInstanceState.canRunEvent(event)) {
                result = currentInstanceState;
            } else if (currentInstanceState instanceof ProtoStateWhile) {
                resetInstanceIfEmptyAndNotBlocked(currentInstanceState, currentInstance);
                result = runLoopState(currentInstance, event);
            } else if (currentInstanceState instanceof ProtoStateSwitchCase) {
                resetInstanceIfEmptyAndNotBlocked(currentInstanceState, currentInstance);
                result = runChoiceState(currentInstance, event);
            } else if (currentInstanceState instanceof ProtoStateSequence) {
                resetInstanceIfEmptyAndNotBlocked(currentInstanceState, currentInstance);
                result = runSequenceState(currentInstance, event);

            }
            if (isFailed(result)) {
                continue;
            } else {
                break;
            }
        }
        return result;
    }

    private ProtoState runChoiceState(ProtoStackItem currentInstance, BaseEvent event) {
        if (currentInstance.getSize() != ((SpecialProtoState) currentInstance.getState()).getChildren().size()) {
            executionStack.pop();
            return new FailedState("");
        }
        ProtoState result = null;
        while (currentInstance.canRun()) {
            var candidate = currentInstance.getNextExecutable();
            if (isNormalState(candidate)) {
                if (candidate.canRunEvent(event)) {
                    result = candidate;
                } else {
                    result = new FailedState("Unable to run event", candidate, event);
                }
            } else {
                if (recursionBlocker.contains(candidate.getUuid())) {

                    result = new FailedState("Blocked recursion", candidate, event);
                } else {
                    executionStack.add(new ProtoStackItem(candidate));
                    result = runInternal(event);
                }
            }
            if (isFailed(result)) {
                continue;
            }
            break;
        }
        if (result == null) {
            result = new FailedState("Unable to run event", currentInstance.getState(), event);
        }
        recursionBlocker.add(currentInstance.getState().getUuid());

        var peek = executionStack.peek();
        if (peek.getState().getUuid().equalsIgnoreCase(currentInstance.getState().getUuid()) && !executionStack.empty()) {
            executionStack.pop();
        }

        return result;
    }

    private ProtoState runSequenceState(ProtoStackItem currentInstance, BaseEvent event) {
        ProtoState result = null;
        while (currentInstance.canRun()) {
            var candidate = currentInstance.getNextExecutable();
            if (isNormalState(candidate)) {
                if (candidate.canRunEvent(event)) {
                    result = candidate;
                } else {
                    result = new FailedState("Unable to run event", candidate, event);
                }
            } else {
                if (recursionBlocker.contains(candidate.getUuid())) {

                    result = new FailedState("Blocked recursion", candidate, event);
                } else {
                    executionStack.add(new ProtoStackItem(candidate));
                    result = runInternal(event);
                }
            }
            if (isFailed(result) && candidate.isOptional()) {
                continue;
            }
            break;
        }
        if (currentInstance.isEmpty() || isFailed(result)) {
            recursionBlocker.add(currentInstance.getState().getUuid());
            var peek = executionStack.peek();
            if (peek.getState().getUuid().equalsIgnoreCase(currentInstance.getState().getUuid()) && !executionStack.empty()) {
                executionStack.pop();
            }
        }


        return result;
    }

    protected void postExecute() {

    }

    public void runException(Exception ex) {
        BaseEvent event = null;
        ProtoState state = null;
        if (ex instanceof FailedStateException) {
            event = ((FailedStateException) ex).getEvent();
            state = ((FailedStateException) ex).getState();
        }
        var exceptionResults = runExceptionInternal(ex, state, event);
        for (var exceptionResult : exceptionResults) {
            log.error("Message: " + exceptionResult.getClass().getSimpleName());
            write(exceptionResult);
        }
    }

    protected List<ReturnMessage> runExceptionInternal(Exception ex, ProtoState state, BaseEvent event) {
        throw new RuntimeException(ex);
    }

    private Optional<ProtoState> getPossibleInterrupt(BaseEvent currentEvent) {
        return this.descriptor.getInterrupts().stream()
                .filter(s -> s.canHandle(currentEvent.getClass()))
                .filter(s -> s.canRunEvent(currentEvent))
                .findFirst();
    }

    public boolean isTransaction() {
        return transaction;
    }

    public void setTransaction(boolean transaction) {
        this.transaction = transaction;
    }
}
