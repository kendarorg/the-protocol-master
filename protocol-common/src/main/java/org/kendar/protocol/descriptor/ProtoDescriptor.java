package org.kendar.protocol.descriptor;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;

import java.util.ArrayList;
import java.util.List;


public abstract class ProtoDescriptor {

    private final List<ProtoState> interrupts = new ArrayList<>();

    protected ProtoState start;

    public void initialize() {
        initializeProtocol();
    }

    protected void initialize(ProtoState start) {

        this.start = start;
    }

    protected abstract void initializeProtocol();


    public ProtoContext buildContext() {
        return createContext(this);
    }

    protected abstract ProtoContext createContext(ProtoDescriptor protoDescriptor);


    public List<ProtoState> getInterrupts() {
        return interrupts;
    }

    protected void addInterruptState(ProtoState currentState) {
        if (!(currentState instanceof InterruptProtoState)) {
            throw new RuntimeException(currentState.getClass().getSimpleName() + " is not an InterruptProtoState");
        }
        interrupts.add(currentState);
    }


    public ProtoState getStart() {
        return start;
    }
}
