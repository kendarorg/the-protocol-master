package org.kendar.protocol;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.protocol.fsm.IntertwinedProtoState;
import org.kendar.protocol.fsm.ProtoLine;
import org.kendar.protocol.fsm.ProtoState;
import org.kendar.proxy.Proxy;
import org.kendar.server.Channel;

import java.util.*;

public abstract class ProtoDescriptor {

    private final Map<Class<?>, ProtoLine> protoLines = new HashMap<>();
    private final List<ProtoState> intertwined = new ArrayList<>();

    private Proxy proxyInstance;
    private boolean proxy;

    public void initialize() {
        if (isProxy()) {
            proxyInstance.initialize();
        }
        initializeProtocol();
    }

    protected abstract void initializeProtocol();

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxyInstance) {
        this.proxyInstance = proxyInstance;
        this.proxyInstance.setProtocol(this);
        this.proxy = true;
    }

    public boolean sendImmediateGreeting() {
        return false;
    }

    public abstract boolean isBe();

    public abstract int getPort();

    public ProtoContext buildContext(Channel client) {
        var result = createContext(this, client);
        if (isProxy()) {
            var conn = proxyInstance.connect();
            result.setValue("CONNECTION", conn);
            result.setProxy(proxyInstance);
        }
        return result;
    }

    protected abstract ProtoContext createContext(ProtoDescriptor protoDescriptor, Channel client);

    protected void addState(ProtoState currentState, ProtoState... possibleStates) {
        var protoLine = new ProtoLine(currentState, possibleStates);
        protoLines.put(currentState.getClass(), protoLine);
    }

    protected void addIntertwinedState(ProtoState currentState) {
        if(!(currentState instanceof IntertwinedProtoState)){
            throw new RuntimeException(currentState.getClass().getSimpleName()+" is not an IntertwinedProtoState");
        }
        intertwined.add(currentState);
    }

    public BBuffer buildBuffer() {
        return new BBuffer(isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }


    public <T> List<ProtoState> getPossibleNext(Class<T> current) {
        List<ProtoState> result = new ArrayList<>();
        if (protoLines.get(current) == null) {
            result.add(new MissingState<T>());
        }else {
            result.addAll(protoLines.get(current).getPossibleStates());
        }
        result.addAll(intertwined);
        return result;
    }
}
