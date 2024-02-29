package org.kendar.proto.fsm;

import org.kendar.proto.silly.SillyTest;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public class ToIntOne extends ProtoState implements ReturnMessage {
    public static boolean run = true;

    public ToIntOne(Class<?> bytesEventClass) {
        super(bytesEventClass);
    }

    public boolean canRun(BytesEvent event) {
        return event.getBuffer().size() > 0 && event.getBuffer().get(0) == '1';
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        SillyTest.result += "ToIntOne";
        event.getBuffer().setPosition(1);
        return iteratorOfList(this);
    }
}
