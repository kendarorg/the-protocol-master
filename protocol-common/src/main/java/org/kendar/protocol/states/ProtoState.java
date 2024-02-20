package org.kendar.protocol.states;

import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.messages.RunnableStep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class ProtoState {
    private String uuid;

    private boolean optional;
    private final Set<Class<?>> messages;

    public ProtoState(Class<?>... messages) {
        this.uuid = UUID.randomUUID().toString();

        this.messages = new HashSet<>(Arrays.asList(messages));
        Class<?> current;
        String exceptionMessage = "";
        try {
            for (var message : messages) {
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

    public static Iterator<ProtoStep> iteratorOfEmpty() {
        return iteratorOfList(new ProtoStep[0]);
    }

    public static Iterator<ProtoStep> iteratorOfRunnable(Supplier<ReturnMessage> step) {
        return iteratorOfRunner(new RunnableStep(step));
    }

    public static Iterator<ProtoStep> iteratorOfRunnable(Runnable step) {
        return iteratorOfRunner(new RunnableStep(step));
    }

    public static Iterator<ProtoStep> iteratorOfList(ProtoStep... steps) {
        return Arrays.asList(steps).iterator();
    }

    public static Iterator<ProtoStep> iteratorOfRunner(ProtoStep... steps) {
        return Arrays.asList(steps).iterator();
    }

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

    public boolean canHandle(Class<?> clazz) {
        return messages.contains(clazz);
    }

    public boolean canRunEvent(BaseEvent event) {
        Method meth;
        try {
            meth = getClass().getMethod("canRun", event.getClass());
        } catch (NoSuchMethodException e) {
            return false;
        }
        try {
            return (boolean) meth.invoke(this, event);
        }catch (IllegalAccessException | InvocationTargetException e) {
            if(e.getCause() instanceof AskMoreDataException){
                throw new AskMoreDataException();
            }
            throw new RuntimeException(e.getCause());
        }
    }

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
}
