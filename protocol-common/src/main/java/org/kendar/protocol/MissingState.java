package org.kendar.protocol;

import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public class MissingState<T> extends ProtoState {
    public boolean canRun(T event){return false;}
    public Iterator<ProtoStep> execute(T event) { return iteratorOfEmpty();}
}
