package org.kendar.protocol.states;

import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.messages.RunnableStep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Basic state
 */
public abstract class ProtoState {

    /**
     * List of handled messages types
     */
    private final Set<Class<?>> messages;

    /**
     * The id of the state (unique for each protocol instance)
     */
    private String uuid;

    /**
     * If the state is optional it can fail without error
     */
    private boolean optional;

    public ProtoState(Class<?>... messages) {
        this.uuid = ProtoDescriptor.getCounterString("PROTO_STATE_COUNTER");

        this.messages = new HashSet<>(Arrays.asList(messages));
        Class<?> current;
        String exceptionMessage = "";
        try {
            for (var message : messages) {
                //Check that it can handle all needed messages
                current = message;
                exceptionMessage = "Missing method " + this.getClass().getSimpleName() + "::canRun(" + current.getSimpleName() + ") ";
                getClass().getMethod("canRun", message);
                exceptionMessage = "Missing method " + this.getClass().getSimpleName() + "::execute(" + current.getSimpleName() + ") ";
                getClass().getMethod("execute", message);
            }
        } catch (Exception e) {
            throw new RuntimeException(exceptionMessage + e.getMessage(), e);
        }
    }

    /**
     * Iterator utils-empty
     *
     * @return
     */
    public static Iterator<ProtoStep> iteratorOfEmpty() {
        return iteratorOfRunner();
    }

    /**
     * Iterator utils directly with return messages, they will be executed in the runSteps phase
     * one by one. They can therefore be interrupted
     *
     * @param step
     * @return
     */
    public static Iterator<ProtoStep> iteratorOfRunnable(Supplier<ReturnMessage> step) {
        return iteratorOfRunner(new RunnableStep(step));
    }

    /**
     * Iterator utils runnable items, they will be executed in the runSteps phase
     * one by one. They can therefore be interrupted
     *
     * @param step
     * @return
     */
    public static Iterator<ProtoStep> iteratorOfRunnable(Runnable step) {
        return iteratorOfRunner(new RunnableStep(step));
    }

    /**
     * Iterator utils ProtoSteps items, they will be executed in the runSteps phase
     * one by one. They can therefore be interrupted
     *
     * @param steps
     * @return
     */
    public static Iterator<ProtoStep> iteratorOfRunner(ProtoStep... steps) {
        return Arrays.asList(steps).iterator();
    }

    /**
     * Iterator of iterators
     *
     * @param returnMessages
     * @return
     */
    public static Iterator<ProtoStep> iteratorOfList(Iterator<ReturnMessage> returnMessages) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return returnMessages.hasNext();
            }

            @Override
            public ProtoStep next() {
                return returnMessages::next;
            }
        };
    }

    /**
     * ITerator of return messages
     *
     * @param msg
     * @return
     */
    public static Iterator<ProtoStep> iteratorOfList(ReturnMessage... msg) {
        return Arrays.stream(msg).map(a -> (ProtoStep) () -> a).collect(Collectors.toList()).iterator();
    }

    public ProtoState asOptional() {
        optional = true;
        return this;
    }

    public ProtoState withUUID(String name) {
        uuid = name;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * Check if the state can handle the specific class
     *
     * @param clazz
     * @return
     */
    public boolean canHandle(Class<?> clazz) {
        return messages.contains(clazz);
    }

    /**
     * Check if the state can handle the specific event
     *
     * @param event
     * @return
     */
    public boolean canRunEvent(BaseEvent event) {
        Method meth;
        try {
            meth = getClass().getMethod("canRun", event.getClass());
        } catch (NoSuchMethodException e) {
            return false;
        }
        try {
            return (boolean) meth.invoke(this, event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (e.getCause() instanceof AskMoreDataException) {
                //In case of missing data ask for more.
                throw new AskMoreDataException();
            }
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Execute the event
     *
     * @param event
     * @return
     */
    public Iterator<ProtoStep> executeEvent(BaseEvent event) {
        Method meth;
        try {
            meth = getClass().getMethod("execute", event.getClass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        try {
            return (Iterator<ProtoStep>) meth.invoke(this, event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
