package org.kendar.protocol.states;

import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

/**
 * Helper interface to manage handled events. It is not used to understand what
 * events can be handled by the state
 *
 * @param <T>
 */
public interface HandlerDefinition<T extends ProtocolEvent> {
    boolean canRun(T event);

    Iterator<ProtoStep> execute(T event);
}
