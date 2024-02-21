package org.kendar.proto.fsm;

import org.kendar.proto.SillyTest;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public class Interrupt extends ProtoState implements InterruptProtoState, ReturnMessage {
    public Interrupt(Class<?> bytesEventClass) {
        super(bytesEventClass);
    }

    public boolean canRun(BytesEvent event) {
        return event.getBuffer().size() > 0 && event.getBuffer().get(0) == 'I';
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        SillyTest.result += "Interrupt";
        event.getBuffer().setPosition(1);
        return iteratorOfList(this);
    }
}
