package org.kendar.proto;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.states.ProtoState;

public abstract class SillyProtocol extends ProtoDescriptor {

    @Override
    protected void initializeProtocol() {
        initialize(doTestInitialize());
    }

    public void addInterruptStateTest(ProtoState currentState) {
        addInterruptState(currentState);
    }

    public abstract ProtoState doTestInitialize();


    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        return new SillyContext(this);
    }
}
