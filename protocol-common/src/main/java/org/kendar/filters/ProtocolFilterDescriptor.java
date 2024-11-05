package org.kendar.filters;

public abstract class ProtocolFilterDescriptor<T,K> implements FilterDescriptor{


    /**
     * @param phase
     * @param request
     * @param response
     * @return true when is blocking
     */
    public abstract boolean handle(ProtocolPhase phase, T in, K out);
}
