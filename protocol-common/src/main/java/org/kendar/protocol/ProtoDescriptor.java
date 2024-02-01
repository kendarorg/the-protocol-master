package org.kendar.protocol;

import org.kendar.protocol.fsm.ProtoLine;
import org.kendar.protocol.fsm.ProtoState;
import org.kendar.proxy.Proxy;
import org.kendar.server.Channel;

import java.util.HashMap;
import java.util.Map;

public abstract class ProtoDescriptor {

    private final Map<Class<?>, ProtoLine> protoLines = new HashMap<>();
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


    public <T> ProtoState[] getPossibleNext(Class<T> current) {
        if (protoLines.get(current) == null) {
            return new ProtoState[]{new MissingState<T>()};
        }
        return protoLines.get(current).getPossibleStates();

    }
}
